package in.reeltime.reel

import in.reeltime.exceptions.AuthorizationException
import in.reeltime.exceptions.ReelNotFoundException
import in.reeltime.exceptions.VideoNotFoundException
import in.reeltime.user.User
import in.reeltime.video.Video
import static in.reeltime.reel.Reel.UNCATEGORIZED_REEL_NAME

class ReelService {

    def userService
    def videoService

    def springSecurityService

    Reel createReel(String reelName) {
        def audience = new Audience(users: [])
        new Reel(name: reelName, audience: audience, videos: [])
    }

    Reel createReelForUser(User owner, String reelName) {
        def reel = createReel(reelName)
        reel.owner = owner
        return reel
    }

    Reel loadReel(Long reelId) {
        def reel = Reel.findById(reelId)
        if(!reel) {
            throw new ReelNotFoundException("Reel [$reelId] not found")
        }
        return reel
    }

    Collection<Reel> listReels(String username) {
        userService.loadUser(username).reels
    }

    void addReel(String reelName) {
        if(reelNameIsUncategorized(reelName)) {
            throw new IllegalArgumentException("Reel name [$reelName] is reserved")
        }
        def currentUser = springSecurityService.currentUser as User
        def reel = createReelForUser(currentUser, reelName)

        currentUser.addToReels(reel)
        userService.storeUser(currentUser)
    }

    void deleteReel(Long reelId) {
        def reel = loadReel(reelId)
        def name = reel.name

        if(currentUserIsNotReelOwner(reel)) {
            throw new AuthorizationException("Only the owner of a reel can delete it")
        }
        else if(reelNameIsUncategorized(name)) {
            throw new AuthorizationException("The ${name} reel cannot be deleted")
        }
        reel.owner.removeFromReels(reel)
        reel.delete()
    }

    void addVideo(Long reelId, Long videoId) {

        def reel = loadReel(reelId)
        def video = videoService.loadVideo(videoId)

        if(currentUserIsNotReelOwner(reel)) {
            throw new AuthorizationException("Only the owner of a reel can add videos to it")
        }

        reel.addToVideos(video)
        reel.save()
    }

    void removeVideo(Long reelId, Long videoId) {

        def reel = loadReel(reelId)
        def video = videoService.loadVideo(videoId)

        if(currentUserIsNotReelOwner(reel)) {
            throw new AuthorizationException("Only the owner of a reel can remove videos from it")
        }
        else if(!reel.containsVideo(video)) {
            throw new VideoNotFoundException("Reel [$reelId] does not contain video [$videoId]")
        }

        reel.removeFromVideos(video)
        reel.save()
    }

    Collection<Video> listVideos(Long reelId) {
        loadReel(reelId).videos
    }

    boolean reelNameIsUncategorized(String reelName) {
        return reelName.toLowerCase() == UNCATEGORIZED_REEL_NAME.toLowerCase()
    }

    private boolean currentUserIsNotReelOwner(Reel reel) {
        def currentUser = springSecurityService.currentUser as User
        return reel.owner != currentUser
    }
}