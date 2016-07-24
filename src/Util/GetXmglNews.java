package Util;

import Model.ArticleItem;
import Model.SimpleArticleItem;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class GetXmglNews {

    // 获取包含了多个栏目的集合
    public static Elements getTables(OkHttpClient client) {

        // get index page
        String url = "http://xmgl.ahau.edu.cn/";
        Elements table = null;

        Request request
                = new Request.Builder()
                .url(url)
                .build();
        try {
            Response responses = client.newCall(request).execute();
            if (responses.isSuccessful()) {

                String result = responses.body().string();
                Document doc = Jsoup.parse(result, "UTF-8");
                table = doc.getElementsByTag("table");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return table;
    }


    public static List getTargetList(int type, Elements tables,
                                     OkHttpClient client, boolean isFull) {
        Element list;
        List articleList = null;
        switch (type) {
            case 1:
                // 通知公告
                list = tables.get(2);
                articleList = GetXmglNews.getObjectList(client, list, type, isFull);
                return articleList;
            case 2:
                // 技术体系
                list = tables.get(5);
                articleList = GetXmglNews.getObjectList(client, list, type, isFull);
                return articleList;
            case 3:
                // 产业动态
                list = tables.get(9);
                articleList = GetXmglNews.getObjectList(client, list, type, isFull);
                return articleList;
            case 6:
                // 图片区
                list = tables.get(6);
                list = list.getElementById("oTransContainer");
                articleList = GetXmglNews.getObjectList(client, list, type, isFull);
                return articleList;
            default:
                return articleList;
        }
    }


    /**
     * 解析链接列表，得到model文章列表的对象List及其id
     * 传入给后面进行逐个解析，生成单篇文章对象
     */
    public static List getObjectList(OkHttpClient client, Element list, int type, boolean isFull) {
        Elements urls = list.getElementsByAttribute("href");
        String url;
        List objList = new ArrayList();
        String url_prefix = "http://xmgl.ahau.edu.cn/";
        int post_id;

        // 如果isFull为假,获取SimpleArticle对象
        if (!isFull) {
            SimpleArticleItem sampItem = null;
            for (Element element : urls) {
                url = url_prefix + element.attr("href");
                String[] tmpString;
                tmpString = url.split("[=]");
                post_id = Integer.parseInt(tmpString[1]);
                sampItem = getSamArticleByUrl(client, url, post_id, type);
                objList.add(sampItem);
            }
        } else {
            ArticleItem fullItem;
            for (Element element : urls) {
                url = url_prefix + element.attr("href");
                String[] tmpString;
                tmpString = url.split("[=]");
                post_id = Integer.parseInt(tmpString[1]);
                fullItem = getfullArticleByUrl(client, url, post_id, type);
                objList.add(fullItem);
            }
        }
        return objList;
    }


    /**
     * 发送请求获取详细的新闻内容
     * 生成model对象ArticleItem
     */
    public static SimpleArticleItem getSamArticleByUrl(OkHttpClient client, String url, int post_id, int type) {

        // 摘要的截取起始和终止位置
        int summaryStart = 0, summaryEnd = 60;

        Request getArticle
                = new Request.Builder()
                .url(url)
                .build();

        Response responArtical = null;
        String detail = null;
        try {
            responArtical = client.newCall(getArticle).execute();
            detail = responArtical.body().string();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Document docDetail = Jsoup.parse(detail);
        String[] author_time = new String[0];
        try {
            author_time = docDetail.getElementById("zmWriteMan").text().split(" ");
        } catch (NullPointerException e) {
        }

        String pubTime;
        // 防止作者与发布时间为空
        if (2 == author_time.length) {
            pubTime = author_time[1];
        } else {
            pubTime = " ";
        }
        String title = docDetail.getElementById("zmTitle").text();
        int readTimes = 0;

        Elements imageElements = docDetail.select("img");
        String imageUrl;
        String[] imageUrls;
        List<String> imageUrlList = new ArrayList<>();
        String urlPrefix = "http://xmgl.ahau.edu.cn/";

        for (Element imageElement : imageElements) {
            String[] tmp = imageElement.attr("src").split("/");
            imageUrl = tmp[1] + "/" + tmp[2];
            imageUrl = urlPrefix + imageUrl;
            Pattern pattern = Pattern.compile("\\d+");
            Matcher matcher = pattern.matcher(tmp[2]);
            while (matcher.find()) {
                post_id = Integer.parseInt(matcher.group(0));
            }
            imageUrlList.add(imageUrl);
        }

        int size = imageUrlList.size();
        imageUrls = imageUrlList.toArray(new String[size]);

        String contentElem = docDetail.getElementById("zmShow").text();
        String content, summary;
        if (contentElem.isEmpty()) {
            return new SimpleArticleItem(post_id, imageUrls, title, pubTime, readTimes, type);
        } else {
            content = docDetail.getElementById("zmShow").text();
            summary = content.substring(summaryStart, summaryEnd);
            return new SimpleArticleItem(post_id, imageUrls, title, pubTime, readTimes, summary, type);
        }
    }


    public static ArticleItem getfullArticleByUrl(OkHttpClient client, String url, int articleID, int type) {

        Request getArticle
                = new Request
                .Builder()
                .url(url)
                .build();

        Response responArtical;
        String detail = null;
        try {
            responArtical = client.newCall(getArticle).execute();
            detail = responArtical.body().string();
        } catch (IOException e) {
            e.printStackTrace();
        }

        Document docDetail = Jsoup.parse(detail);
        String[] author_time = {};
        try {
            author_time = docDetail.getElementById("zmWriteMan").text().split(" ");
        } catch (NullPointerException e) {
        }

        String pubTime;
        // 防止作者与发布时间为空
        if (2 == author_time.length) {
            pubTime = author_time[1];
        } else {
            pubTime = " ";
        }
        String title = docDetail.getElementById("zmTitle").text();
        int readTimes = 0;

        Elements imageElements = docDetail.select("img");
        String imageUrl;
        String[] imageUrls;
        List<String> imageUrlList = new ArrayList<>();
        String imgUrlPrefix = "http://xmgl.ahau.edu.cn/";

        for (Element imageElement : imageElements) {
            String[] tmp = imageElement.attr("src").split("/");
            imageUrl = tmp[1] + "/" + tmp[2];
            imageUrl = imgUrlPrefix + imageUrl;
            imageUrlList.add(imageUrl);
        }

        int size = imageUrlList.size();
        imageUrls = imageUrlList.toArray(new String[size]);
        // 不使用String content = docDetail.getElementById("zmShow").text();
        // 因为使用toString会保留html标记，使用已有的文本格式
        String content = docDetail.getElementById("zmShow").toString();
        String source = "蜜蜂病虫害监测风险评估预警系统";

        return new ArticleItem(articleID, imageUrls, title, pubTime, readTimes, source, content, type);
    }
}