package com.kodak;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.CountDownLatch;

/**
 * Hello world!
 */
public class App {

    private static Logger log = LogManager.getLogger(App.class);

    public static void main(String[] args) {
        try {
            Document doc = Jsoup.connect("http://127.0.0.1/1").get();

            log.info("连接网络中...");

            Elements eles = doc.select("a");
            if (eles != null && !eles.isEmpty()) {
                Element ele = eles.get(0);
                String version = ele.text();
                String url = ele.attr("href");

                File file = new File("history_file.txt");
                if (!file.exists()) {
                    file.createNewFile();
                }
                BufferedReader bufferedReader = new BufferedReader(new FileReader("history_file.txt"));
                String lastVersion = bufferedReader.readLine();
                boolean downloaded = false;
                while (lastVersion != null) {
                    if (lastVersion.equals(version)) {
                        downloaded = true;
                        break;
                    }
                    lastVersion = bufferedReader.readLine();
                }
                if (lastVersion == null || !downloaded) {
                    log.info(version + "尚未下载, 即将开始下载");
                    File dir = new File(version);
                    if (!dir.exists()) {
                        dir.mkdir();
                    }
                    String path = dir.getAbsolutePath();
                    Document versionDoc = Jsoup.connect(url).get();
                    Elements fileElems = versionDoc.select("a");
                    if (fileElems != null && !fileElems.isEmpty()) {
                        for (int i = 0; i < fileElems.size(); i++) {
                            Element fileElem = fileElems.get(i);
                            String fileuri = fileElem.attr("href");
                            String filen = fileElem.text();
                            if ((filen.contains(".exe") || filen.contains(".zip")) && filen.contains("BLD")) {
                                downloadFile(
                                        fileuri,
                                        path,
                                        fileuri.split("//")[1].split("/")[fileuri.split("//")[1].split("/").length - 1]
                                );
                            }
                        }

                        FileWriter fileWriter = new FileWriter("history_file.txt", true);
                        BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
                        bufferedWriter.append(version);
                        bufferedWriter.flush();
                        bufferedWriter.close();
                    }
                } else {
                    log.info(version + "文件已下载");
                }
            }

        } catch (IOException e) {
            // TODO Auto-generated catch block
            log.error(e);
        }
    }

    /**
     *  下载文件
     * @param uri ：下载的URL路径
     * @param path ：保存到本地的文件夹路径
     * @param filename ： 保存文件的命名名字
     */
    private static void downloadFile(String uri, String path, String filename) {
        try {
            String fileName = path + "\\" + filename;
            log.info(fileName);
            URL url = new URL(uri);
            HttpURLConnection http = (HttpURLConnection) url.openConnection();

            RandomAccessFile osf = null;
            /**
             * 此处设定5个线程下载一个文件tn = 5;
             * 判断平均每个线程需下载文件长度：
             */
            log.info("file size:" + http.getContentLength());
            int tn = 3;
            int bn = 0;
            int len = http.getContentLength() / tn;//舍去余数（余数自动舍去）计算每个线程应下载平均长度，最后一个线程再加上余数，则是整个文件的长度,
            File f = new File(fileName);
            if (f.exists()) {
                f.delete();
                osf = new RandomAccessFile(f, "rw");
                osf.seek(http.getContentLength() - 1);
                osf.write(0);
            } else {
                osf = new RandomAccessFile(f, "rw");
                osf.seek(http.getContentLength() - 1);
                osf.write(0);
            }
            log.info("temp 文件长度：" + f.length());
            Thread t;//下载子线程，
            CountDownLatch latch = new CountDownLatch(3);
            for (int j = 0; j < tn; j++) {
                if (j == tn - 1) {//如果最后一个线程则加上余数长度字节
                    bn = len + (http.getContentLength() % tn);
                } else {
                    bn = len;
                }
                log.info("t" + j + "线程下载长度：" + bn + "起始字节：" + len * j);
                t = new DownloadThread(
                        j,
                        uri,
                        fileName,
                        len * j,
                        bn,
                        latch
                );
                t.start();
            }
            latch.await();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            log.error(e);
        } catch (InterruptedException e) {
            log.error(e);
        }
    }
}
