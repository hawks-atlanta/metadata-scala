# Metadata (Scala)

[![Release](https://github.com/hawks-atlanta/metadata-scala/actions/workflows/release.yaml/badge.svg?branch=main)](https://github.com/hawks-atlanta/metadata-scala/actions/workflows/release.yaml)
[![Tagging](https://github.com/hawks-atlanta/metadata-scala/actions/workflows/tagging.yaml/badge.svg?branch=dev)](https://github.com/hawks-atlanta/metadata-scala/actions/workflows/tagging.yaml)
[![codecov](https://codecov.io/gh/hawks-atlanta/metadata-scala/graph/badge.svg?token=M9CJCEEIBK)](https://codecov.io/gh/hawks-atlanta/metadata-scala)

## Development

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
| ------------------------------------------------------------ | ------------------------------------------------------------ |
