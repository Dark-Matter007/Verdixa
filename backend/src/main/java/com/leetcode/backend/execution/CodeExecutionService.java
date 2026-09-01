package com.leetcode.backend.execution;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.concurrent.TimeUnit;

@Service
public class CodeExecutionService {

    private static final long TIME_LIMIT_SECONDS = 5;
    // Windows/MSYS2 can take longer to cold-start g++ under normal system load.
    // This governs compilation only; submitted programs retain the five-second runtime limit.
    private static final long COMPILATION_TIME_LIMIT_SECONDS = 30;

    // =========================================================
    // WINDOWS COMPILER / INTERPRETERS
    // =========================================================

    @Value("${algosphere.execution.javac:javac}")
    private String javacCommand;
    @Value("${algosphere.execution.java:java}")
    private String javaCommand;
    @Value("${algosphere.execution.gxx:g++}")
    private String gxxCommand;
    @Value("${algosphere.execution.python:python}")
    private String pythonCommand;
    @Value("${algosphere.execution.runtime-path:}")
    private String runtimePath;

    // =========================================================
    // JAVA
    // =========================================================

    public ExecutionResult executeJava(
            String sourceCode,
            String inputData) {

        long startTime = System.currentTimeMillis();
        Path tempDirectory = null;

        try {

            tempDirectory =
                    Files.createTempDirectory(
                            "algosphere-java-"
                    );

            Path sourceFile =
                    tempDirectory.resolve(
                            "Solution.java"
                    );

            Files.writeString(
                    sourceFile,
                    sourceCode,
                    StandardCharsets.UTF_8
            );

            // -------------------------------------------------
            // COMPILE JAVA
            // -------------------------------------------------

            ProcessBuilder compileBuilder =
                    new ProcessBuilder(
                            javacCommand,
                            sourceFile.toString()
                    );

            compileBuilder.redirectErrorStream(true);

            Process compileProcess =
                    compileBuilder.start();

            boolean compileFinished =
                    compileProcess.waitFor(
                            COMPILATION_TIME_LIMIT_SECONDS,
                            TimeUnit.SECONDS
                    );

            String compileOutput =
                    readProcessOutput(
                            compileProcess
                    );

            if (!compileFinished) {

                compileProcess.destroyForcibly();

                return new ExecutionResult(
                        "COMPILATION_ERROR",
                        "",
                        "Java compilation timed out.",
                        elapsed(startTime)
                );
            }

            if (compileProcess.exitValue() != 0) {

                return new ExecutionResult(
                        "COMPILATION_ERROR",
                        "",
                        compileOutput,
                        elapsed(startTime)
                );
            }

            // -------------------------------------------------
            // RUN JAVA
            // -------------------------------------------------

            ProcessBuilder runBuilder =
                    new ProcessBuilder(
                            javaCommand,
                            "-cp",
                            tempDirectory.toString(),
                            "Solution"
                    );

            runBuilder.redirectErrorStream(true);

            Process runProcess =
                    runBuilder.start();

            writeInput(
                    runProcess,
                    inputData
            );

            boolean finished =
                    runProcess.waitFor(
                            TIME_LIMIT_SECONDS,
                            TimeUnit.SECONDS
                    );

            String output =
                    readProcessOutput(
                            runProcess
                    );

            if (!finished) {

                runProcess.destroyForcibly();

                return new ExecutionResult(
                        "TIME_LIMIT_EXCEEDED",
                        output,
                        "Java program exceeded the time limit.",
                        elapsed(startTime)
                );
            }

            if (runProcess.exitValue() != 0) {

                return new ExecutionResult(
                        "RUNTIME_ERROR",
                        output,
                        "Java program terminated with an error.",
                        elapsed(startTime)
                );
            }

            return new ExecutionResult(
                    "EXECUTED",
                    output,
                    "",
                    elapsed(startTime)
            );

        } catch (Exception e) {

            return new ExecutionResult(
                    "EXECUTION_ERROR",
                    "",
                    e.getMessage(),
                    elapsed(startTime)
            );

        } finally {

            if (tempDirectory != null) {
                deleteDirectory(tempDirectory);
            }
        }
    }

    // =========================================================
    // BACKWARD-COMPATIBLE JAVA
    // =========================================================

    public ExecutionResult executeJava(
            String sourceCode) {

        return executeJava(
                sourceCode,
                ""
        );
    }

    // =========================================================
    // C++
    // =========================================================

    public ExecutionResult executeCpp(
            String sourceCode,
            String inputData) {

        long startTime = System.currentTimeMillis();
        Path tempDirectory = null;

        try {

            // -------------------------------------------------
            // TEMP DIRECTORY
            // -------------------------------------------------

            tempDirectory =
                    Files.createTempDirectory(
                            "algosphere-cpp-"
                    );

            Path sourceFile =
                    tempDirectory.resolve(
                            "Solution.cpp"
                    );

            Path executable =
                    tempDirectory.resolve(
                            "Solution.exe"
                    );

            // -------------------------------------------------
            // WRITE SOURCE
            // -------------------------------------------------

            Files.writeString(
                    sourceFile,
                    sourceCode,
                    StandardCharsets.UTF_8
            );

            // =================================================
            // COMPILE C++
            // =================================================

            ProcessBuilder compileBuilder =
                    new ProcessBuilder(
                            gxxCommand,
                            "-std=c++17",
                            "-O2",
                            sourceFile.toString(),
                            "-o",
                            executable.toString()
                    );

            // -------------------------------------------------
            // IMPORTANT:
            // Add MSYS2 UCRT64 environment to PATH.
            // -------------------------------------------------

            String currentPath =
                    compileBuilder.environment()
                            .getOrDefault(
                                    "PATH",
                                    ""
                            );

            if (!runtimePath.isBlank()) compileBuilder.environment().put("PATH", runtimePath + File.pathSeparator + currentPath);

            compileBuilder.redirectErrorStream(true);

            Process compileProcess =
                    compileBuilder.start();

            boolean compileFinished =
                    compileProcess.waitFor(
                            COMPILATION_TIME_LIMIT_SECONDS,
                            TimeUnit.SECONDS
                    );

            String compileOutput =
                    readProcessOutput(
                            compileProcess
                    );

            // -------------------------------------------------
            // COMPILATION TIMEOUT
            // -------------------------------------------------

            if (!compileFinished) {

                compileProcess.destroyForcibly();

                return new ExecutionResult(
                        "COMPILATION_ERROR",
                        "",
                        "C++ compilation timed out.",
                        elapsed(startTime)
                );
            }

            // -------------------------------------------------
            // COMPILATION ERROR
            // -------------------------------------------------

            if (compileProcess.exitValue() != 0) {

                return new ExecutionResult(
                        "COMPILATION_ERROR",
                        "",
                        compileOutput,
                        elapsed(startTime)
                );
            }

            // -------------------------------------------------
            // VERIFY EXECUTABLE
            // -------------------------------------------------

            if (!Files.exists(executable)) {

                return new ExecutionResult(
                        "COMPILATION_ERROR",
                        "",
                        "C++ compiler finished successfully, "
                                + "but executable was not created.",
                        elapsed(startTime)
                );
            }

            // =================================================
            // RUN C++
            // =================================================

            ProcessBuilder runBuilder =
                    new ProcessBuilder(
                            executable.toString()
                    );

            // -------------------------------------------------
            // IMPORTANT:
            // C++ executable needs UCRT64 DLLs at runtime.
            // -------------------------------------------------

            String runPath =
                    runBuilder.environment()
                            .getOrDefault(
                                    "PATH",
                                    ""
                            );

            if (!runtimePath.isBlank()) runBuilder.environment().put("PATH", runtimePath + File.pathSeparator + runPath);

            runBuilder.redirectErrorStream(true);

            Process runProcess =
                    runBuilder.start();

            // -------------------------------------------------
            // SEND INPUT
            // -------------------------------------------------

            writeInput(
                    runProcess,
                    inputData
            );

            // -------------------------------------------------
            // WAIT
            // -------------------------------------------------

            boolean finished =
                    runProcess.waitFor(
                            TIME_LIMIT_SECONDS,
                            TimeUnit.SECONDS
                    );

            String output =
                    readProcessOutput(
                            runProcess
                    );

            // -------------------------------------------------
            // TIME LIMIT
            // -------------------------------------------------

            if (!finished) {

                runProcess.destroyForcibly();

                return new ExecutionResult(
                        "TIME_LIMIT_EXCEEDED",
                        output,
                        "C++ program exceeded the time limit.",
                        elapsed(startTime)
                );
            }

            // -------------------------------------------------
            // RUNTIME ERROR
            // -------------------------------------------------

            if (runProcess.exitValue() != 0) {

                return new ExecutionResult(
                        "RUNTIME_ERROR",
                        output,
                        "C++ program terminated with an error.",
                        elapsed(startTime)
                );
            }

            // -------------------------------------------------
            // SUCCESS
            // -------------------------------------------------

            return new ExecutionResult(
                    "EXECUTED",
                    output,
                    "",
                    elapsed(startTime)
            );

        } catch (Exception e) {

            return new ExecutionResult(
                    "EXECUTION_ERROR",
                    "",
                    e.getMessage(),
                    elapsed(startTime)
            );

        } finally {

            if (tempDirectory != null) {
                deleteDirectory(tempDirectory);
            }
        }
    }

    // =========================================================
    // BACKWARD-COMPATIBLE C++
    // =========================================================

    public ExecutionResult executeCpp(
            String sourceCode) {

        return executeCpp(
                sourceCode,
                ""
        );
    }

    // =========================================================
    // PYTHON
    // =========================================================

    public ExecutionResult executePython(
            String sourceCode,
            String inputData) {

        long startTime = System.currentTimeMillis();
        Path tempDirectory = null;

        try {

            // -------------------------------------------------
            // TEMP DIRECTORY
            // -------------------------------------------------

            tempDirectory =
                    Files.createTempDirectory(
                            "algosphere-python-"
                    );

            Path sourceFile =
                    tempDirectory.resolve(
                            "solution.py"
                    );

            Files.writeString(
                    sourceFile,
                    sourceCode,
                    StandardCharsets.UTF_8
            );

            // -------------------------------------------------
            // RUN PYTHON
            // -------------------------------------------------

            ProcessBuilder runBuilder =
                    new ProcessBuilder(
                            pythonCommand,
                            sourceFile.toString()
                    );

            runBuilder.redirectErrorStream(true);

            Process runProcess =
                    runBuilder.start();

            // -------------------------------------------------
            // SEND INPUT
            // -------------------------------------------------

            writeInput(
                    runProcess,
                    inputData
            );

            // -------------------------------------------------
            // WAIT
            // -------------------------------------------------

            boolean finished =
                    runProcess.waitFor(
                            TIME_LIMIT_SECONDS,
                            TimeUnit.SECONDS
                    );

            String output =
                    readProcessOutput(
                            runProcess
                    );

            // -------------------------------------------------
            // TIME LIMIT
            // -------------------------------------------------

            if (!finished) {

                runProcess.destroyForcibly();

                return new ExecutionResult(
                        "TIME_LIMIT_EXCEEDED",
                        output,
                        "Python program exceeded the time limit.",
                        elapsed(startTime)
                );
            }

            // -------------------------------------------------
            // RUNTIME ERROR
            // -------------------------------------------------

            if (runProcess.exitValue() != 0) {

                return new ExecutionResult(
                        "RUNTIME_ERROR",
                        output,
                        "Python program terminated with an error.",
                        elapsed(startTime)
                );
            }

            // -------------------------------------------------
            // SUCCESS
            // -------------------------------------------------

            return new ExecutionResult(
                    "EXECUTED",
                    output,
                    "",
                    elapsed(startTime)
            );

        } catch (Exception e) {

            return new ExecutionResult(
                    "EXECUTION_ERROR",
                    "",
                    e.getMessage(),
                    elapsed(startTime)
            );

        } finally {

            if (tempDirectory != null) {
                deleteDirectory(tempDirectory);
            }
        }
    }

    // =========================================================
    // BACKWARD-COMPATIBLE PYTHON
    // =========================================================

    public ExecutionResult executePython(
            String sourceCode) {

        return executePython(
                sourceCode,
                ""
        );
    }

    // =========================================================
    // WRITE INPUT
    // =========================================================

    private void writeInput(
            Process process,
            String inputData)
            throws IOException {

        try (
                BufferedWriter writer =
                        new BufferedWriter(
                                new OutputStreamWriter(
                                        process.getOutputStream(),
                                        StandardCharsets.UTF_8
                                )
                        )
        ) {

            if (inputData != null &&
                    !inputData.isEmpty()) {

                writer.write(inputData);

                if (!inputData.endsWith("\n")) {
                    writer.newLine();
                }
            }

            writer.flush();
        }
    }

    // =========================================================
    // READ OUTPUT
    // =========================================================

    private String readProcessOutput(
            Process process)
            throws IOException {

        try (
                InputStream inputStream =
                        process.getInputStream()
        ) {

            return new String(
                    inputStream.readAllBytes(),
                    StandardCharsets.UTF_8
            );
        }
    }

    // =========================================================
    // ELAPSED TIME
    // =========================================================

    private long elapsed(
            long startTime) {

        return System.currentTimeMillis()
                - startTime;
    }

    // =========================================================
    // DELETE TEMP DIRECTORY
    // =========================================================

    private void deleteDirectory(
            Path directory) {

        try {

            if (Files.exists(directory)) {

                Files.walk(directory)
                        .sorted(
                                (a, b) ->
                                        b.compareTo(a)
                        )
                        .forEach(path -> {

                            try {

                                Files.deleteIfExists(
                                        path
                                );

                            } catch (IOException ignored) {
                            }

                        });
            }

        } catch (IOException ignored) {
        }
    }
}
