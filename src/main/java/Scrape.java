public class Scrape {

    public static void main(String[] args) {
        ScrapeUtil.getHTML("https://www.facebook.com/livecandid/videos/733698873897654/", false, true);
        ScrapeUtil.getHTML("https://www.facebook.com/watch/?v=800204267223116", false, true);
        //ScrapeUtil.getHTML("https://www.facebook.com/watch/?v=3487136038067660", false, true);
        //ScrapeUtil.getHTML("https://www.facebook.com/watch/?v=1042311479538975", false, true);
    }
}
