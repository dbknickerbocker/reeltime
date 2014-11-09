package test.helper

import in.reeltime.user.User
import in.reeltime.video.Video

class VideoFactory {

    static Video createVideoAndWait(User creator, String title, long millisecondsToWait) {
        def video = createVideo(creator, title)
        sleep(millisecondsToWait)
        return video
    }

    static Video createVideo(User creator, String title, boolean available = true) {
        def video = new Video(title: title, masterPath: title + '-path', available: available)
        creator.addToVideos(video)
        video.save()
    }
}
