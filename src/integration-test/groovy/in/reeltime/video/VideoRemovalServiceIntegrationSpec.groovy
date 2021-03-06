package in.reeltime.video

import grails.plugin.springsecurity.SpringSecurityUtils
import grails.test.mixin.integration.Integration
import grails.transaction.Rollback
import in.reeltime.exceptions.AuthorizationException
import in.reeltime.maintenance.ResourceRemovalTarget
import in.reeltime.playlist.*
import in.reeltime.reel.Reel
import in.reeltime.reel.ReelVideo
import in.reeltime.thumbnail.Thumbnail
import in.reeltime.thumbnail.ThumbnailResolution
import in.reeltime.thumbnail.ThumbnailStorageService
import in.reeltime.thumbnail.ThumbnailVideo
import in.reeltime.transcoder.TranscoderJob
import in.reeltime.user.User
import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Specification
import spock.lang.Unroll
import in.reeltime.test.factory.UserFactory

@Integration
@Rollback
class VideoRemovalServiceIntegrationSpec extends Specification {

    @Autowired
    VideoRemovalService videoRemovalService

    @Autowired
    VideoStorageService videoStorageService

    @Autowired
    ThumbnailStorageService thumbnailStorageService

    @Autowired
    PlaylistAndSegmentStorageService playlistAndSegmentStorageService

    User creator
    Reel reel

    String videoBase
    String thumbnailBase
    String playlistBase

    void setup() {
        videoBase = videoStorageService.videoBase
        thumbnailBase = thumbnailStorageService.thumbnailBase
        playlistBase = playlistAndSegmentStorageService.playlistBase
    }

    void "video owner must be the currently logged in user requesting deletion"() {
        given:
        setupData()

        and:
        def notCreator = UserFactory.createUser('notCreator')

        and:
        def video = new Video(
                title: 'some video',
                masterPath: 'something.mp4',
                masterThumbnailPath: 'something.png'
        ).save()

        new VideoCreator(video: video, creator: creator).save()

        when:
        SpringSecurityUtils.doWithAuth(notCreator.username) {
            videoRemovalService.removeVideo(video)
        }

        then:
        def e = thrown(AuthorizationException)
        e.message == 'Only the creator of a video can delete it'
    }

    @Unroll
    void "remove video by id [#removeById] successfully and schedule resources for removal"() {
        given:
        setupData()

        and:
        def playlist = new Playlist().save()

        def segment1 = new Segment(segmentId: 1, uri: 'seg1.ts', duration: '1.0').save()
        def segment2 = new Segment(segmentId: 2, uri: 'seg2.ts', duration: '1.0').save()

        new PlaylistSegment(playlist: playlist, segment: segment1).save()
        new PlaylistSegment(playlist: playlist, segment: segment2).save()

        assert playlist.segments.size() == 2

        def video = new Video(
                title: 'some video',
                masterPath: 'something.mp4',
                masterThumbnailPath: 'something.png'
        ).save()

        new VideoCreator(video: video, creator: creator).save()

        def thumbnail1 = new Thumbnail(resolution: ThumbnailResolution.RESOLUTION_1X, uri: 'thumbnail-1x').save()
        def thumbnail2 = new Thumbnail(resolution: ThumbnailResolution.RESOLUTION_2X, uri: 'thumbnail-2x').save()
        def thumbnail3 = new Thumbnail(resolution: ThumbnailResolution.RESOLUTION_3X, uri: 'thumbnail-3x').save()

        new ThumbnailVideo(thumbnail: thumbnail1, video: video).save()
        new ThumbnailVideo(thumbnail: thumbnail2, video: video).save()
        new ThumbnailVideo(thumbnail: thumbnail3, video: video).save()

        new PlaylistVideo(playlist: playlist, video: video).save()

        def playlistUri1 = new PlaylistUri(type: PlaylistType.Variant, uri: 'variant.m3u8').save()
        def playlistUri2 = new PlaylistUri(type: PlaylistType.Media, uri: 'media.m3u8').save()

        new PlaylistUriVideo(playlistUri: playlistUri1, video: video).save()
        new PlaylistUriVideo(playlistUri: playlistUri2, video: video).save()

        def transcoderJob = new TranscoderJob(video: video, jobId: '1234567890123-ABCDEF').save()

        and:
        assert playlist.id > 0
        assert reel.id > 0
        assert transcoderJob.id > 0
        assert video.id > 0

        and:
        def videoId = video.id
        def playlistId = playlist.id

        def segment1Id = segment1.id
        def segment2Id = segment2.id

        def playlistUri1Id = playlistUri1.id
        def playlistUri2Id = playlistUri2.id

        def thumbnail1Id = thumbnail1.id
        def thumbnail2Id = thumbnail2.id
        def thumbnail3Id = thumbnail3.id

        def reelId = reel.id
        def transcoderJobId = transcoderJob.id

        when:
        SpringSecurityUtils.doWithAuth(creator.username) {
            if (removeById) {
                videoRemovalService.removeVideoById(videoId)
            }
            else {
                videoRemovalService.removeVideo(video)
            }
        }

        then:
        Video.findById(videoId) == null
        Playlist.findById(playlistId) == null

        Segment.findById(segment1Id) == null
        Segment.findById(segment2Id) == null

        PlaylistUri.findById(playlistUri1Id) == null
        PlaylistUri.findById(playlistUri2Id) == null

        Thumbnail.findById(thumbnail1Id) == null
        Thumbnail.findById(thumbnail2Id) == null
        Thumbnail.findById(thumbnail3Id) == null

        and:
        VideoCreator.findByVideo(video) == null

        PlaylistUriVideo.findAllByVideo(video).empty
        PlaylistVideo.findAllByVideo(video).empty

        PlaylistSegment.findAllByPlaylist(playlist).empty
        ThumbnailVideo.findAllByVideo(video).empty

        and:
        Reel.findById(reelId) != null
        ReelVideo.findByReelAndVideo(reel, video) == null

        and:
        TranscoderJob.findById(transcoderJobId) == null

        and:
        ResourceRemovalTarget.findByBaseAndRelative(videoBase, 'something.mp4') != null
        ResourceRemovalTarget.findByBaseAndRelative(thumbnailBase, 'something.png') != null

        ResourceRemovalTarget.findByBaseAndRelative(thumbnailBase, 'thumbnail-1x') != null
        ResourceRemovalTarget.findByBaseAndRelative(thumbnailBase, 'thumbnail-2x') != null
        ResourceRemovalTarget.findByBaseAndRelative(thumbnailBase, 'thumbnail-3x') != null

        ResourceRemovalTarget.findByBaseAndRelative(playlistBase, 'seg1.ts') != null
        ResourceRemovalTarget.findByBaseAndRelative(playlistBase, 'seg2.ts') != null

        ResourceRemovalTarget.findByBaseAndRelative(playlistBase, 'variant.m3u8') != null
        ResourceRemovalTarget.findByBaseAndRelative(playlistBase, 'media.m3u8') != null

        where:
        _   |   removeById
        _   |   false
        _   |   true
    }

    private void setupData() {
        creator = UserFactory.createTestUser()
        reel = creator.reels[0]
    }
}
