import Model.ArticleItem;
import Model.SimpleArticleItem;
import Util.GetXmglNews;
import com.mongodb.*;
import com.squareup.okhttp.OkHttpClient;
import org.jsoup.select.Elements;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;


/**
 * Created by anderson on 7/24/16.
 */
public class BeeNews {
    public static void main(String[] args) throws UnknownHostException {

        List<Integer> types = new ArrayList<Integer>() {{
            add(1);
            add(2);
            add(3);
            add(6);
        }};

        MongoClient sampMongoClient;
        DB sampDB;
        // 简单表和完整表
        // 分别用于展示新闻列表和新闻详情
        DBCollection sampleColl;
        DBCollection fullColl;
        BasicDBObject doc;

        // String hostName = "localhost";
        String hostName = "localhost";
        int port = 27017;
        String dbName = "BeeNews";
        String sampCollectName = "SimpleArticle";
        String fullCollectName = "FullArticle";
        WriteConcern writeConcern = WriteConcern.JOURNALED;
        List<Boolean> isFull = new ArrayList<Boolean>() {{
            add(true);
            add(false);
        }};

        String userName = "XXXX";
        char[] password = "XXXX".toCharArray();


        MongoCredential credential = MongoCredential.createCredential(userName, dbName, password);
        sampMongoClient = new MongoClient(new ServerAddress(hostName, port), Arrays.asList(credential));

        // Connect to mongo DB.
        sampDB = sampMongoClient.getDB(dbName);
        sampleColl = sampDB.getCollection(sampCollectName);
        fullColl = sampDB.getCollection(fullCollectName);

        sampMongoClient.setWriteConcern(writeConcern);

        // 获取首页各个栏目的集合
        OkHttpClient client = new OkHttpClient();
        Elements table = GetXmglNews.getTables(client);

        for (Boolean bool : isFull) {
            for (int type : types) {

                BasicDBObject query = new BasicDBObject("type", type);
                BasicDBObject sortRe = new BasicDBObject("aid", -1);
                int maxId;

                // 提取该类型栏目的文章对象列表
                if (!bool) {
                    List<SimpleArticleItem> list = GetXmglNews.getTargetList(type, table, client, bool);
                    // 获取最大的文章ID，只有当ID大于它时，再插入文章。
                    try {
                        maxId = (int) sampleColl.find(query).sort(sortRe).next().get("aid");
                    } catch (NoSuchElementException e) {
                        maxId = 0;
                    }
                    // 将Java对象解析为BasicDBObject以便存入MongoDB
                    for (SimpleArticleItem item : list) {
                        if (item.getId() > maxId) {
                            doc = new BasicDBObject
                                    ("aid", item.getId())
                                    .append("type", item.getType())
                                    .append("title", item.getTitle())
                                    .append("readTime", item.getReadTimes())
                                    .append("publishDate", item.getPublishDate())
                                    .append("summary", item.getSummary())
                                    .append("imageUrls", item.getImageUrls());

                            sampleColl.insert(doc);
                            System.out.println(doc);
                        }
                    }
                } else {
                    try {
                        maxId = (int) fullColl.find(query).sort(sortRe).next().get("aid");
                    } catch (NoSuchElementException e) {
                        maxId = 0;
                    }
                    List<ArticleItem> list = GetXmglNews.getTargetList(type, table, client, bool);
                    // 将Java对象解析为BasicDBObject以便存入MongoDB
                    for (ArticleItem item : list) {
                        if (item.getId() > maxId) {
                            doc = new BasicDBObject
                                    ("aid", item.getId())
                                    .append("type", item.getType())
                                    .append("title", item.getTitle())
                                    .append("readTime", item.getReadTimes())
                                    .append("publishDate", item.getPublishDate())
                                    .append("imageUrls", item.getImageUrls())
                                    .append("source", item.getSource())
                                    .append("content", item.getBody());

                            fullColl.insert(doc);
                            System.out.println(doc);
                        }
                    }
                }
            }
        }
    }
}