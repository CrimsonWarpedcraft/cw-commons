"""Verify the files deployed by maven-publish, independently of Gradle's classpath."""

import hashlib
import re
import sys
import tomllib
import xml.etree.ElementTree as ET
from pathlib import Path
from zipfile import ZipFile


NATIVE_GROUP = "com.crimsonwarpedcraft.cwcommons"
JITPACK_GROUP = "com.github.CrimsonWarpedcraft"
ARTIFACT = "cw-commons"
MAVEN = "{http://maven.apache.org/POM/4.0.0}"
DRIVER = "META-INF/services/java.sql.Driver"
CLASS_PREFIXES = ("com/crimsonwarpedcraft/cwcommons/", "org/sqlite/")


def verify_jar(path):
    with ZipFile(path) as jar:
        unexpected = []
        for entry in jar.infolist():
            if entry.is_dir():
                continue
            name = re.sub(r"^META-INF/versions/[0-9]+/", "", entry.filename)
            if name.endswith(".class") and not name.startswith(CLASS_PREFIXES):
                unexpected.append(entry.filename)
            elif name.startswith("META-INF/services/") and entry.filename != DRIVER:
                unexpected.append(entry.filename)
        if unexpected:
            raise ValueError("Unexpected classes or service descriptors:\n  " + "\n  ".join(sorted(unexpected)))

        for name in (
            "com/crimsonwarpedcraft/cwcommons/config/ConfigManager.class",
            "com/crimsonwarpedcraft/cwcommons/store/Repository.class",
            "org/sqlite/JDBC.class",
            "org/sqlite/native/Linux/x86_64/libsqlitejdbc.so",
            "org/sqlite/native/Windows/x86_64/sqlitejdbc.dll",
            "org/sqlite/native/Mac/x86_64/libsqlitejdbc.dylib",
        ):
            if jar.getinfo(name).file_size == 0:
                raise ValueError(f"Empty JAR entry: {name}")
        providers = [line.partition("#")[0].strip() for line in jar.read(DRIVER).decode().splitlines()]
        if jar.namelist().count(DRIVER) != 1 or list(filter(None, providers)) != ["org.sqlite.JDBC"]:
            raise ValueError(f"Expected one JDBC descriptor providing org.sqlite.JDBC, got {providers}")


def verify_pom(path, group, version, versions):
    pom = ET.parse(path).getroot()
    gav = tuple(pom.findtext(MAVEN + field) for field in ("groupId", "artifactId", "version"))
    if gav != (group, ARTIFACT, version):
        raise ValueError(f"Wrong coordinates in {path}: {gav}")

    # The template also declares both libraries directly; a consumer build alone can mask a bad POM.
    expected = [
        dict(groupId="com.fasterxml.jackson.dataformat", artifactId="jackson-dataformat-yaml",
             version=versions["jackson"], scope="compile"),
        dict(groupId="org.hibernate.validator", artifactId="hibernate-validator",
             version=versions["hibernate-validator"], scope="compile"),
    ]
    actual = [{c.tag.removeprefix(MAVEN): c.text for c in dep}
              for dep in pom.findall(f"{MAVEN}dependencies/{MAVEN}dependency")]
    if sorted(actual, key=lambda dep: dep.get("groupId", "")) != expected:
        raise ValueError(f"Wrong API dependencies in {path}: {actual}")


def verify_publication(repo, version):
    catalog = Path(__file__).resolve().parents[1] / "gradle/libs.versions.toml"
    with catalog.open("rb") as file:
        versions = tomllib.load(file)["versions"]
    digests = []
    for group in (NATIVE_GROUP, JITPACK_GROUP):
        directory = repo / group.replace(".", "/") / ARTIFACT / version
        stem = f"{ARTIFACT}-{version}"
        verify_pom(directory / f"{stem}.pom", group, version, versions)
        jar = directory / f"{stem}.jar"
        verify_jar(jar)
        with ZipFile(directory / f"{stem}-javadoc.jar") as docs:
            if docs.getinfo("com/crimsonwarpedcraft/cwcommons/store/Repository.html").file_size == 0:
                raise ValueError("Empty repository API documentation")
        with jar.open("rb") as file:
            digests.append(hashlib.file_digest(file, "sha256").hexdigest())
        print(f"Verified {group}:{ARTIFACT}:{version}  SHA-256 {digests[-1]}")
    if digests[0] != digests[1]:
        raise ValueError("The JitPack-group JAR differs from the native publication")


if __name__ == "__main__":
    if len(sys.argv) != 3:
        sys.exit("Usage: verify_publication.py <staged Maven repository> <candidate version>")
    try:
        verify_publication(Path(sys.argv[1]), sys.argv[2])
    except (ValueError, KeyError, OSError, ET.ParseError) as error:
        sys.exit(f"Candidate verification failed: {error}")
