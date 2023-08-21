# Metadata (Scala)

[![Release](https://github.com/hawks-atlanta/metadata-scala/actions/workflows/release.yaml/badge.svg?branch=main)](https://github.com/hawks-atlanta/metadata-scala/actions/workflows/release.yaml)
[![Tagging](https://github.com/hawks-atlanta/metadata-scala/actions/workflows/tagging.yaml/badge.svg?branch=dev)](https://github.com/hawks-atlanta/metadata-scala/actions/workflows/tagging.yaml)
[![codecov](https://codecov.io/gh/hawks-atlanta/metadata-scala/graph/badge.svg?token=M9CJCEEIBK)](https://codecov.io/gh/hawks-atlanta/metadata-scala)

## Development

### Local database

1. Run the `docker-compose` command: 

```bash
docker-compose -f docker-compose.dev.yml up
```

2. (Optional) Open the `pgadmin` page in `http://localhost:5050/` and login with the following credentials:

| Email                 | Password |
| --------------------- | -------- |
| postgres@postgres.com | postgres |

Note that sometimes the `pgadmin` container doesn't start properly, so you'll need to run the command again. This usually occurs the first time you run the command.

3. (Optional) Create a new server in `pgadmin` with the following credentials:

| Field                | Value       |
|----------------------|-------------|
| Host                 | postgres-db |
| Port                 | 5432        |
| Maintenance database | metadata    |
| User                 | postgres    |
| Password             | postgres    |

### Create packages

You can create new packages with default folders (`domain`, `application`, `infraestructure` and `test`) using the following command:

```bash
make create
```

After running the command you'll be prompted to enter the name of the package.

### Remove packages

You can remove packages using the following command:

```bash
make remove
```

After running the command you'll be prompted to enter the name of the package.

## Tests

1. Make sure you have `sbt` installed in your computer:

```bash
sbt --version
```

2. Run the tests and generate the coverage report:

```bash
sbt clean coverage test coverageReport
```

3. (Optional) Open the `html` coverage file located in:

```bash
cd target/scala-2.13/scoverage-report
```

## Coverage

| [![circle](https://codecov.io/gh/hawks-atlanta/metadata-scala/graphs/sunburst.svg?token=M9CJCEEIBK)](https://app.codecov.io/gh/hawks-atlanta/metadata-scala) | [![square](https://codecov.io/gh/hawks-atlanta/metadata-scala/graphs/tree.svg?token=M9CJCEEIBK)](https://app.codecov.io/gh/hawks-atlanta/metadata-scala) |
| ------------------------------------------------------------------------------------------------------------------------------------------------------------ | -------------------------------------------------------------------------------------------------------------------------------------------------------- |
