package in.reeltime.activity

import grails.test.spock.IntegrationSpec
import in.reeltime.reel.Reel
import in.reeltime.user.User
import in.reeltime.video.Video
import test.helper.UserFactory

class ActivityServiceIntegrationSpec extends IntegrationSpec {

    def activityService

    User user
    Reel reel
    Video video

    void setup() {
        user = UserFactory.createTestUser()
        reel = user.reels[0]
        video = new Video(creator: user, title: 'title', masterPath: 'masterPath', available: true).save()
    }

    void "save reel creation activity"() {
        when:
        activityService.reelCreated(user, reel)

        then:
        def activity = UserReelActivity.findByUserAndReel(user, reel)
        activity != null

        activity instanceof CreateReelActivity
        activity.type == ActivityType.CreateReel
    }

    void "save video added to reel activity"() {
        when:
        activityService.videoAddedToReel(user, reel, video)

        then:
        def activity = UserReelActivity.findByUserAndReel(user, reel)
        activity != null

        activity instanceof AddVideoToReelActivity
        activity.type == ActivityType.AddVideoToReel
    }

    void "no user activities to delete"() {
        when:
        activityService.deleteAllUserActivity(user)

        then:
        notThrown(Exception)
    }

    void "delete all user activity"() {
        given:
        def activity1 = new CreateReelActivity(user: user, reel: reel).save()
        def activity2 = new CreateReelActivity(user: user, reel: reel).save()

        and:
        def activityId1 = activity1.id
        assert UserReelActivity.findById(activityId1) != null

        and:
        def activityId2 = activity2.id
        assert UserReelActivity.findById(activityId2) != null

        when:
        activityService.deleteAllUserActivity(user)

        then:
        UserReelActivity.findById(activityId1) == null
        UserReelActivity.findById(activityId2) == null
    }

    void "empty criteria for activities"() {
        when:
        def list = activityService.findActivities([], [])

        then:
        list.size() == 0
    }

    void "no activities matching criteria"() {
        when:
        def list = activityService.findActivities([user], [reel])

        then:
        list.size() == 0
    }

    void "list activity by user and reel returns mixed list"() {
        given:
        activityService.reelCreated(user, reel)
        activityService.videoAddedToReel(user, reel, video)

        expect:
        assertFindActivities([user], [reel])
        assertFindActivities([user], [])
        assertFindActivities([], [reel])
    }

    private void assertFindActivities(List<User> users, List<Reel> reels) {
        def list = activityService.findActivities(users, reels)
        assert list.size() == 2

        assert list[0].type == ActivityType.CreateReel
        assert list[1].type == ActivityType.AddVideoToReel
    }
}