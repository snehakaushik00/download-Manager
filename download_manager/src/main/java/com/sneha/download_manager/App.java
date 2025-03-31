package com.sneha.download_manager;

/**
 * Hello world!
 *
 */
import java.net.MalformedURLException;
import java.net.URL;

public class App {
    public static void main(String[] args) {
        DownloadManager manager = new DownloadManager();

        try {
            // Create download tasks
            DownloadTask task1 = new DownloadTask(
                    "1",
                    new URL("https://dlcdn.apache.org/maven/maven-3/3.9.9/binaries/apache-maven-3.9.9-bin.zip"),
                    "archive_1.zip",
                    1,
                    4,
                    new RetryPolicy(3));

              DownloadTask task2 = new DownloadTask(
                    "2",
                    new URL("https://storage.googleapis.com/kaggle-data-sets/6133903/9970426/bundle/archive.zip?X-Goog-Algorithm=GOOG4-RSA-SHA256&X-Goog-Credential=gcp-kaggle-com%40kaggle-161607.iam.gserviceaccount.com%2F20241204%2Fauto%2Fstorage%2Fgoog4_request&X-Goog-Date=20241204T201858Z&X-Goog-Expires=259200&X-Goog-SignedHeaders=host&X-Goog-Signature=b70482b2d278baccc72a4253cae08e91d43b82d4d20315beb890d07804edebbeb540ee54ff6af8a4453cd82aa9ed20de7e3f12211d3e73a629f7c5e9d04709ca92c5248f02aa36cccddba266ec7aee38ffba23df6fca72dffde10f4d1ed0ea3e9b122bf9a0583b2d6bbd680068c2fbc8e4b2c00fe99f7445df38d07a5d14a3b5f11afe39634c9209568eca2ec4854b8f8dd94decad335a78b778d6ce0d48f4bb93161943f103ba046cd838d5f5da3deb0e47f00797ba08d8f89f4ac0c01b729ac8abf9174721c93af8a34b1f4341fb4cc497a900962f56f3a95792631cce2db9dde0c3564fd652ad5a83242f38b553c65e6759b5c05f5594c087cb4b75b850ab"),
                    "archive_2.zip",
                    2,
                    4,
                    new RetryPolicy(2));

            manager.addDownload(task1);
            manager.addDownload(task2);
            manager.startDownload("1");
            manager.startDownload("2");

            Thread.sleep(5000);
            manager.pauseDownload("1");
            manager.pauseDownload("2");

            Thread.sleep(3000);
            manager.startDownload("1");
            manager.startDownload("2");

        } catch (MalformedURLException | InterruptedException e) {
            e.printStackTrace();
        } finally {
            manager.shutdown();
            System.out.println("Manager Shutdown.");
        }
    }
}