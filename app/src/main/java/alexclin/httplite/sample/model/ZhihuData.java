package alexclin.httplite.sample.model;

import java.util.Arrays;
import java.util.List;

/**
 * alexclin.httplite.sample.model
 *
 * @author alexclin
 * @date 16/1/2 16:01
 */
public class ZhihuData {
    public String date;
    public List<Story> stories;
    public List<TopStory> top_stories;

    @Override
    public String toString() {
        return "ZhihuData{" +
                "date='" + date + '\'' +
                ", stories=" + stories +
                '}';
    }

    public static class Story{
        public String[] images;
        public int type;
        public long id;
        public String ga_prefix;
        public String title;
        public boolean multipic;

        @Override
        public String toString() {
            return "Story{" +
                    "images=" + Arrays.toString(images) +
                    ", type=" + type +
                    ", id=" + id +
                    ", ga_prefix='" + ga_prefix + '\'' +
                    ", title='" + title + '\'' +
                    ", multipic=" + multipic +
                    '}';
        }
    }

    public static class TopStory{
        String images;
        int type;
        long id;
        String ga_prefix;
        String title;
        public boolean multipic;

        @Override
        public String toString() {
            return "TopStory{" +
                    "images='" + images + '\'' +
                    ", type=" + type +
                    ", id=" + id +
                    ", ga_prefix='" + ga_prefix + '\'' +
                    ", title='" + title + '\'' +
                    ", multipic=" + multipic +
                    '}';
        }
    }
}
