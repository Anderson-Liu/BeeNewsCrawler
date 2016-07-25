import Model.ArticleItem;
import Model.SimpleArticleItem;
import Util.Constant;
import Util.GetXmglNews;
import com.mongodb.MongoClient;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.mongodb.WriteConcern;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.squareup.okhttp.OkHttpClient;
import org.bson.Document;
import org.jsoup.select.Elements;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.mongodb.client.model.Projections.excludeId;
import static com.mongodb.client.model.Sorts.descending;


/**
 * Created by anderson on 7/24/16.
 */
public class BeeNewsCrawler {
    public static void main(String[] args) throws UnknownHostException {

        List<Integer> types = new ArrayList<Integer>() {{
            add(1);
            add(2);
            add(3);
            add(6);
        }};

        MongoClient sampMongoClient;
        MongoDatabase sampDB;
        // 简单表和完整表
        // 分别用于展示新闻列表和新闻详情
        MongoCollection<Document> sampleColl;
        MongoCollection<Document> fullColl;
        MongoCollection<Document> imgColl;
        Document doc;

        WriteConcern writeConcern = WriteConcern.JOURNALED;
        List<Boolean> isFull = new ArrayList<Boolean>() {{
            add(true);
            add(false);
        }};

        MongoCredential credential = MongoCredential.createCredential(Constant.MONGO_USER, Constant.MONGO_DB, Constant.MONGO_PASS);
        sampMongoClient = new MongoClient(new ServerAddress(Constant.MONGO_HOST, Constant.MONGO_PORT), Arrays.asList(credential));

        // Connect to mongo DB.
        sampDB = sampMongoClient.getDatabase(Constant.MONGO_DB);
        sampleColl = sampDB.getCollection(Constant.SAMP_COLLECTION);
        fullColl = sampDB.getCollection(Constant.FULL_COLLECTION);
        imgColl = sampDB.getCollection(Constant.IMG_COLLECTION);

        sampMongoClient.setWriteConcern(writeConcern);

        // 获取首页各个栏目的集合
        OkHttpClient client = new OkHttpClient();
        Elements table = GetXmglNews.getTables(client);

        // 先进行详细文章，几个类别的添加
        // 再进行simple文章，几个类别的添加
        for (Boolean bool : isFull) {
            for (int type : types) {

                // 如果抓取的是图片栏，则更新进数据库的现存文章中
                // 否则就添加文章进入数据库
                if (6 == type) {
                    if (!bool) {
                        List<SimpleArticleItem> list = GetXmglNews.getTargetList(type, table, client, bool);
                        for (SimpleArticleItem item : list) {
                            Document query = new Document("title", item.getTitle());
                            Document oriDocument = fullColl.find(query).first();
                            if (null == oriDocument) {
                                System.out.println("找不到图片对应的文章, 图片标题：" + item.getTitle());
                                continue;
                            }

                            // 新文档保留旧文档的aid, type, readTime, publishDate, summary
                            // 只更新tile, imageUrls
                            Document modifDocument = new Document
                                    ("aid", oriDocument.getInteger("aid"))
                                    .append("type", oriDocument.getInteger("type"))
                                    .append("title", item.getTitle())
                                    .append("readTime", oriDocument.getInteger("readTime"))
                                    .append("publishDate", oriDocument.getDate("publishDate"))
                                    .append("summary", oriDocument.getString("summary"))
                                    .append("imageUrls", item.getImageUrls());
                            fullColl.replaceOne(new Document("title", item.getTitle()), modifDocument);
                            // 将图片集中拷贝进另一张表里，方便
                            imgColl.insertOne(new Document("imageUrls", item.getImageUrls()));
                        }
                    } else {
                        List<ArticleItem> list = GetXmglNews.getTargetList(type, table, client, bool);
                        for (ArticleItem item : list) {
                            Document query = new Document("title", item.getTitle());
                            Document oriDocument = fullColl.find(query).projection(excludeId()).first();
                            if (null == oriDocument) {
                                System.out.println("找不到图片对应的文章, 图片标题：" + item.getTitle());
                                continue;
                            }

                            // 新文档保留旧文档的aid, type, readTime, publishDate, source, content
                            // 只更新tile, imageUrls
                            try {
                                Document modifDocument = new Document()
                                        .append("aid", oriDocument.getInteger("aid"))
                                        .append("type", oriDocument.getInteger("type"))
                                        .append("title", item.getTitle())
                                        .append("readTime", oriDocument.getInteger("readTime"))
                                        .append("publishDate", oriDocument.getDate("publishDate"))
                                        .append("source", oriDocument.getString("Source"))
                                        .append("content", oriDocument.getString("content"))
                                        .append("imageUrls", item.getImageUrls());

                                fullColl.replaceOne(new Document("title", item.getTitle()), modifDocument);
                            } catch (NullPointerException e) {
                                e.printStackTrace();
                            }
                        }
                    }

                } else {

                    int maxId;
                    // 提取该类型栏目的文章对象列表
                    if (!bool) {
                        List<SimpleArticleItem> list = GetXmglNews.getTargetList(type, table, client, bool);
                        // 获取最大的文章ID，只有当ID大于它时，再插入文章。
                        try {
                            maxId = fullColl.find().sort(descending("aid")).first().getInteger("aid");
                        } catch (NullPointerException e) {
                            maxId = 0;
                        }
                        // 将Java对象解析为BasicDBObject以便存入MongoDB
                        for (SimpleArticleItem item : list) {
                            if (item.getId() > maxId) {
                                doc = new Document
                                        ("aid", item.getId())
                                        .append("type", item.getType())
                                        .append("title", item.getTitle())
                                        .append("readTime", item.getReadTimes())
                                        .append("publishDate", item.getPublishDate())
                                        .append("summary", item.getSummary())
                                        .append("imageUrls", item.getImageUrls());

                                sampleColl.insertOne(doc);
                                System.out.println(doc);
                            }
                        }
                    } else {
                        try {
                            maxId = fullColl.find().sort(descending("aid")).first().getInteger("aid");
                        } catch (NullPointerException e) {
                            maxId = 0;
                        }
                        List<ArticleItem> list = GetXmglNews.getTargetList(type, table, client, bool);
                        // 将Java对象解析为BasicDBObject以便存入MongoDB
                        for (ArticleItem item : list) {
                            if (item.getId() > maxId) {
                                doc = new Document
                                        ("aid", item.getId())
                                        .append("type", item.getType())
                                        .append("title", item.getTitle())
                                        .append("readTime", item.getReadTimes())
                                        .append("publishDate", item.getPublishDate())
                                        .append("source", item.getSource())
                                        .append("content", item.getBody())
                                        .append("imageUrls", item.getImageUrls());

                                fullColl.insertOne(doc);
                                System.out.println(doc);
                            }
                        }
                    }
                }
            }
        }
    }
}