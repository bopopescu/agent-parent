package com.eden.agent.service;

import java.util.List;
import java.util.Map;
import java.util.Random;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.eden.agent.common.http.DefaultHttpService;
import com.eden.agent.dao.YoutubeEntityDao;
import com.eden.agent.domain.YoutubeEntity;
import com.google.common.collect.Maps;

/**
 * Create by zhaoxianghui on 2017/11/23.
 */
@Service
public class NewPublishService implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(NewPublishService.class);

    @Value("${web.topbuzz.cookie}")
    private String cookie;
    @Value("${web.publish.keyWord}")
    private String configKeyWord;
    @Value("${web.publish.number.perDay}")
    private Integer numberPerDay;
    @Value("${web.publish.sleep.min.time}")
    private Integer minSleepTime;
    @Value("${web.publish.sleep.max.time}")
    private Integer maxSleepTime;

    private static final String KEY_FETCH_ID = "fetch_id";

    @Autowired
    private YoutubeEntityDao youtubeEntityDao;

    @Override
    public void run() {
        publish();
    }

    public void publish() {

        logger.info("cookie:\n ");
        logger.info(cookie);

        List<YoutubeEntity> list = youtubeEntityDao.selectNotPublish(configKeyWord);

        if (CollectionUtils.isEmpty(list)) {
            logger.info("未找到抓取记录");
            return;
        }

        if (list.size() > numberPerDay) {
            list = list.subList(0, numberPerDay + 1);
        }

        for (YoutubeEntity baseInfo : list) {
            if (baseInfo.getFetchStatus() == 0) {
                fetch(baseInfo);
                sleepForRandomSecond();
            }
            if (baseInfo.getPostStatus() == 0) {
                post(baseInfo);
                sleepForRandomSecond();
            }
            if (baseInfo.getPublishStatus() == 0) {
                doPublish(baseInfo);
                sleepForRandomSecond();
            }
        }

    }

    public YoutubeEntity fetch(YoutubeEntity baseInfo) {

        String url = "https://topbuzz.com/pgc/video/fetch";

        Map<String, String> headerMap = Maps.newHashMap();
        headerMap.put("Cookie", cookie);
        headerMap.put("Connection", "keep-alive");

        logger.info("fetch: " + baseInfo.getVideoLink());
        DefaultHttpService httpService = new DefaultHttpService();
        Map<String, Object> paramsMap = Maps.newHashMap();
        paramsMap.put("video_url", baseInfo.getVideoLink());
        paramsMap.put("article_type", "1");
        String json = httpService.get(url, paramsMap, headerMap);

        logger.info("fetch response:" + json);

        // get fetch id
        String fetchId;
        JSONObject fetchResponseMsg = JSON.parseObject(json).getJSONObject("data");
        if (fetchResponseMsg.containsKey(KEY_FETCH_ID)) {
            fetchId = fetchResponseMsg.getString(KEY_FETCH_ID);
            paramsMap.put(KEY_FETCH_ID, fetchId);
        } else {
            return baseInfo;
        }

        String fetchDataJson = httpService.get(url, paramsMap, headerMap);

        JSONObject data = JSON.parseObject(fetchDataJson).getJSONObject("data").fluentPut("video_file_type", "video");
        String videoTitle = data.getString("video_title");

        int retryTimes = 3;

        while (videoTitle == null && retryTimes < 3) {
            sleepForRandomSecond();
            data = JSON.parseObject(fetchDataJson).getJSONObject("data").fluentPut("video_file_type", "video");
            videoTitle = data.getString("video_title");
        }

        if (videoTitle == null) {
            return baseInfo;
        }

        System.out.println("video title: " + videoTitle);
        baseInfo.setVideoInfo(data.toString());
        baseInfo.setVideoTitle(videoTitle);
        baseInfo.setFetchStatus(1);
        logger.debug("fetch video info: " + data.toString());
        logger.info("fetch success");
        youtubeEntityDao.update(baseInfo);

        return baseInfo;
    }

    public YoutubeEntity post(YoutubeEntity baseInfo) {
        if (baseInfo == null || StringUtils.isBlank(baseInfo.getVideoInfo())) {
            logger.warn("post参数不完整");
            return baseInfo;
        }

        logger.info("post begin : video title:" + baseInfo.getVideoTitle());
        DefaultHttpService httpService = new DefaultHttpService();
        String url = "https://topbuzz.com/pgc/article/publish/post";
        Map<String, Object> paramsMap = Maps.newHashMap();
        paramsMap.put("item_id", "");
        paramsMap.put("publish", "0");
        paramsMap.put("article_type", "1");
        paramsMap.put("title", baseInfo.getVideoTitle());
        paramsMap.put("tags", "");
        paramsMap.put("content", "");
        paramsMap.put("has_video", "0");
        paramsMap.put("video_info", baseInfo.getVideoInfo());
        paramsMap.put("paste_url_count", "1");

        Map<String, String> headerMap = Maps.newHashMap();
        headerMap.put("Cookie", cookie);
        headerMap.put("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/61.0.3163.100 Safari/537.36");
        headerMap.put("Accept", "*/*");
        headerMap.put("Accept-Encoding", "gzip, deflate, br");
        headerMap.put("Accept-Language", "zh-CN,zh;q=0.8");
        headerMap.put("scheme", "https");
        headerMap.put("Connection", "keep-alive");
        headerMap.put("Host", "topbuzz.com");
        headerMap.put("X-Requested-With", "XMLHttpRequest");
        headerMap.put("Referer", "https://topbuzz.com/");

        String json = httpService.post(url, paramsMap, headerMap);
        System.out.println("post response: " + json);
        String itemId = JSON.parseObject(json).getJSONObject("data").getString("item_id");
        baseInfo.setVideoItemId(itemId);
        baseInfo.setPostStatus(1);
        youtubeEntityDao.update(baseInfo);
        logger.info("post finish : video item Id:" + itemId);

        return baseInfo;
    }

    public String doPublish(YoutubeEntity baseInfo) {

        if (baseInfo == null || StringUtils.isBlank(baseInfo.getVideoItemId())) {
            logger.warn("post参数不完整");
            return "publish failed";
        }

        logger.info("publish begin : video title:" + baseInfo.getVideoTitle());
        DefaultHttpService httpService = new DefaultHttpService();
        String url = "https://topbuzz.com/pgc/article/publish/post";
        Map<String, Object> paramsMap = Maps.newHashMap();
        paramsMap.put("item_id", baseInfo.getVideoItemId());
        paramsMap.put("publish", "1");
        paramsMap.put("article_type", "1");
        paramsMap.put("title", baseInfo.getVideoTitle());
        paramsMap.put("tags", "");
        paramsMap.put("content", "");
        paramsMap.put("has_video", "0");
        paramsMap.put("video_info", baseInfo.getVideoInfo());
        paramsMap.put("paste_url_count", "1");

        Map<String, String> headerMap = Maps.newHashMap();
        headerMap.put("Cookie", cookie);
        headerMap.put("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/61.0.3163.100 Safari/537.36");
        headerMap.put("Accept", "*/*");
        headerMap.put("Accept-Encoding", "gzip, deflate, br");
        headerMap.put("Accept-Language", "zh-CN,zh;q=0.8");
        headerMap.put("scheme", "https");
        headerMap.put("Connection", "keep-alive");
        baseInfo.setPublishStatus(1);
        youtubeEntityDao.update(baseInfo);
        return httpService.post(url, paramsMap, headerMap);
    }

    public String getConfigKeyWord() {
        return configKeyWord;
    }

    public void setConfigKeyWord(String configKeyWord) {
        this.configKeyWord = configKeyWord;
    }

    public void sleepForRandomSecond() {
        Random rand = new Random();
        try {
            int time = rand.nextInt(maxSleepTime - minSleepTime) + minSleepTime;
            logger.info("sleep for : " + String.valueOf(time));
            Thread.sleep(time * 1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}