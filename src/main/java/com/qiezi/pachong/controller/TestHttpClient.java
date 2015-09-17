package com.qiezi.pachong.controller;

import com.qiezi.pachong.model.Title;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.htmlparser.NodeFilter;
import org.htmlparser.*;
import org.htmlparser.filters.AndFilter;
import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.HeadingTag;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.tags.TableColumn;
import org.htmlparser.tags.TableRow;
import org.htmlparser.util.NodeList;
import org.htmlparser.util.ParserException;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created by Daniel on 15/9/16.
 */
public class TestHttpClient {

    private String localFile = "./temp.html";
    private List<Title> resultList = new ArrayList<Title>();

    public static void main(String[] args) {
        int pageNum = Integer.parseInt(args[0]);    // 获取初始的参数
        TestHttpClient testHttpClient = new TestHttpClient();
        testHttpClient.task(testHttpClient, pageNum);
        testHttpClient.printResult();
    }

    public void task(TestHttpClient testHttpClient, int pageNum) {

        for (int pageNo = 1; pageNo <= pageNum; pageNo++) {
            List<Title> pageResutl = testHttpClient.downloadPage(pageNo, testHttpClient);
            resultList = mergeResult(pageResutl);   // 将当前页面的合并到最终的结果页面
        }

    }

    // 返回当期页面的前100个
    public List<Title> downloadPage(int pageNo, TestHttpClient testHttpClient) {
        try {
            testHttpClient.get(pageNo);
            List<Title> result = testHttpClient.parserNum();
            return result;
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParserException e) {
            e.printStackTrace();
        }
        return null;
    }

    // 打印结果 并将结果存储在一个文件中
    public void printResult() {
        System.out.println(resultList.size());
        PrintWriter printWriter = null;
        try {
            printWriter = new PrintWriter(new OutputStreamWriter(new FileOutputStream(new File("./result.txt"))), true);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            System.out.println("file not found");
        }
        for (Title title : resultList) {
            printWriter.write("名称 " + title.getTitleName() + '\n');
            printWriter.write("数量 " + title.getCommentNum() + '\n');
            printWriter.write("网址 http://cl.youcl.biz/" + title.getUrl() + '\n');
            printWriter.write("*******************\n");
        }
        printWriter.close();
    }

    // 合并页面，一直提取最大评论数标题
    public List<Title> mergeResult(List<Title> list) {
        List<Title> temp = sortAndTopTitile(list);
        resultList.addAll(temp);
        resultList = sortAndTopTitile(resultList);
        return resultList;
    }

    public List<Title> parserNum() throws IOException, ParserException {

        String html = IOUtils.toString(new FileInputStream(localFile), "GB2312");
        List<Title> result = new ArrayList<Title>();

        Parser parser = new Parser();
        parser.setInputHTML(html);
        // 联合过滤来获取相应的列和行
        NodeFilter[] predicates = new NodeFilter[]{new TagNameFilter("tr"), new HasAttributeFilter("class", "tr3 t_one"), new HasAttributeFilter("align", "center")};
        NodeList trNodeList = parser.extractAllNodesThatMatch(new AndFilter(predicates));

        for (int i = 0; i < trNodeList.size(); i++) {
            // 获取评论数量
            TableRow tableRow = (TableRow) trNodeList.elementAt(i);
            TableColumn[] tableColumn = tableRow.getColumns();
            //             获取文章的评论数量
            int num = Integer.parseInt(tableColumn[3].getStringText());

            // 获取文章的标题
            NodeList nodeList = tableColumn[1].getChildren();
            HeadingTag HeadTag = (HeadingTag) nodeList.elementAt(1);
            LinkTag linkTag = (LinkTag) HeadTag.getChild(0);

            Title title = new Title();
            title.setUrl(linkTag.getLink());
            title.setTitleName(linkTag.getLinkText());
            title.setCommentNum(num);
            result.add(title);
        }
        return result;
    }

    // 对每页的结果进行排序，选取最大的10个项目
    public List<Title> sortAndTopTitile(List<Title> list) {
        List<Title> temp = new ArrayList<Title>();
        Collections.sort(list, new Comparator<Title>() {
            public int compare(Title o1, Title o2) {
                return -(o1.getCommentNum() - o2.getCommentNum());
            }
        });
        for (int i = 0; i < 25; i++) {
            temp.add(list.get(i));
        }
        return temp;
    }

    // 获取网页内容
    public void get(int pageNo) throws IOException {
        CloseableHttpClient httpclient = HttpClients.createDefault();
        String url = "http://cl.youcl.biz/thread0806.php?fid=2&search=&page=" + pageNo;
        HttpGet httpGet = new HttpGet(url);

        CloseableHttpResponse response1 = httpclient.execute(httpGet);
        try {
            System.out.println(response1.getStatusLine());
            HttpEntity entity1 = response1.getEntity();
            InputStream content = entity1.getContent();
            // 将捕获的文件放到内容中去
            org.apache.commons.io.IOUtils.copy(content, new FileOutputStream(localFile));
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(content));
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                System.out.println(line);
            }
            EntityUtils.consume(entity1);

        } finally {
            response1.close();
        }
    }

}

