/*
 * COPYRIGHT Ericsson 2024
 *
 *
 *
 * The copyright to the computer program(s) herein is the property of
 *
 * Ericsson Inc. The programs may be used and/or copied only with written
 *
 * permission from Ericsson Inc. or in accordance with the terms and
 *
 * conditions stipulated in the agreement/contract under which the
 *
 * program(s) have been supplied.
 */
package com.ericsson.amcommonwfs.presentation.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.multipart.MultipartFile;

import com.ericsson.amcommonwfs.utils.repository.FileStorageException;
import com.ericsson.amcommonwfs.utils.repository.TemporaryFileServiceImpl;

@SpringBootTest
@ActiveProfiles("dev")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@TestPropertySource(properties = {"spring.flyway.enabled=false"})
@AutoConfigureObservability
public class TemporaryFileServiceImplTest {

    private static final File TEMP_DIRECTORY = new File(File.separator + "tmp");

    public static final String TEST_FILE_NAME = "testFile";
    public static final String TEST_FILE_EXTENSION = ".txt";
    public static final String TEST_FILE_CONTENT_TYPE = "text/plain";
    public static final String TEST_FILE_CONTENT = "test text";

    @Mock
    private MockMultipartFile mockTestFile;

    private final TemporaryFileServiceImpl temporaryFileStorageService = new TemporaryFileServiceImpl();

    @BeforeEach
    public void setUp() throws IOException {
        doReturn(TEST_FILE_NAME).when(mockTestFile).getName();
        doReturn(TEST_FILE_NAME + TEST_FILE_EXTENSION).when(mockTestFile).getOriginalFilename();
        doReturn(TEST_FILE_CONTENT_TYPE).when(mockTestFile).getContentType();
        doReturn(TEST_FILE_CONTENT.getBytes()).when(mockTestFile).getBytes();

        cleanTempDirectory();
    }

    @Test
    public void shouldSaveTempFile() throws IOException {
        MultipartFile testFile = buildTestFile();
        Path actualFilePath = temporaryFileStorageService.saveFile(testFile);

        assertTempFileCorrect(actualFilePath.toFile());
    }

    @Test
    public void shouldThrowFileStorageExceptionWhenSavingFailed() throws IOException {

        assertThrows(FileStorageException.class, () -> {
            doThrow(IOException.class).when(mockTestFile).transferTo(any(File.class));

            temporaryFileStorageService.saveFile(mockTestFile);
        });
    }

    @Test
    public void shouldRemoveTempFileWhenFileStorageExceptionThrown() throws IOException {
        doThrow(IOException.class).when(mockTestFile).transferTo(any(File.class));
        int expectedTempDirectoryFilesCount = countTempDirectoryFiles();

        try {
            temporaryFileStorageService.saveFile(mockTestFile);
        } catch (FileStorageException ex) {
            assertEquals(expectedTempDirectoryFilesCount, countTempDirectoryFiles());
        }
    }

    @Test
    public void shouldSaveTempFilesWhenSameNames() throws IOException {
        MultipartFile testFile = buildTestFile();
        Path actualFilePath = temporaryFileStorageService.saveFile(testFile);
        Path secondActualFilePath = temporaryFileStorageService.saveFile(testFile);

        assertTempFileCorrect(actualFilePath.toFile());
        assertTempFileCorrect(secondActualFilePath.toFile());
    }

    @Test
    public void shouldRemoveTempFile() {
        Path filePathToRemove = temporaryFileStorageService.saveFile(mockTestFile);

        temporaryFileStorageService.removeFile(filePathToRemove);

        File actualRemovedFile = filePathToRemove.toFile();
        assertFalse(actualRemovedFile.exists());
    }

    @Test
    public void shouldSaveFileFromStringWhenNotExist() throws IOException {
        String newFilePath = temporaryFileStorageService.saveFileIfNotExists(TEST_FILE_CONTENT, TEST_FILE_NAME);

        String actualContent = FileUtils.readFileToString(new File(newFilePath), StandardCharsets.UTF_8.name());

        assertTrue(new File(newFilePath).exists());
        assertEquals(TEST_FILE_CONTENT, actualContent);
    }

    private void assertTempFileCorrect(File actualFile) throws IOException {
        assertTrue(actualFile.exists());
        assertEquals(TEST_FILE_CONTENT, FileUtils.readFileToString(actualFile, "UTF-8"));
    }

    private MockMultipartFile buildTestFile() {
        return new MockMultipartFile(TEST_FILE_NAME, TEST_FILE_NAME + TEST_FILE_EXTENSION,
                                     TEST_FILE_CONTENT_TYPE, TEST_FILE_CONTENT.getBytes());
    }

    private int countTempDirectoryFiles() {
        return TEMP_DIRECTORY.list().length;
    }

    private void cleanTempDirectory() throws IOException {
        File[] filesToDelete = TEMP_DIRECTORY.listFiles((file, fileName) ->
                                                                fileName.startsWith(TEST_FILE_NAME) && fileName.endsWith(TEST_FILE_EXTENSION));

        for (File file : filesToDelete) {
            FileUtils.forceDelete(file);
        }
    }
}