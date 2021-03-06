package in.reeltime.activity

import grails.core.GrailsApplication
import grails.test.mixin.integration.Integration
import grails.test.runtime.DirtiesRuntime
import grails.transaction.Rollback
import in.reeltime.reel.Reel
import in.reeltime.test.factory.ReelFactory
import in.reeltime.test.factory.UserFactory
import in.reeltime.test.factory.VideoFactory
import in.reeltime.user.User
import in.reeltime.video.Video
import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Specification
import spock.lang.Unroll

@Integration
@Rollback
class ActivityServiceIntegrationSpec extends Specification {

    @Autowired
    ActivityService activityService

    @Autowired
    GrailsApplication grailsApplication

    User theUser
    Reel theReel
    Video theVideo

    static final int TEST_MAX_ACTIVITIES_PER_PAGE = 3

    User getUser() {
        if(theUser == null) {
            theUser = UserFactory.createTestUser()
        }
        return theUser
    }

    Reel getUncategorizedReel() {
        user.reels.find { it.name == Reel.UNCATEGORIZED_REEL_NAME }
    }

    Reel getReel() {
        if(theReel == null) {
            theReel = ReelFactory.createReel(user, 'activity-test')
        }
        return theReel
    }

    Video getVideo() {
        if(theVideo == null) {
            theVideo = new Video(creator: user, title: 'title', masterPath: 'masterPath',
                    masterThumbnailPath: 'masterThumbnailPath', available: true).save()
        }
        return theVideo
    }

    void "save reel creation activity"() {
        when:
        activityService.reelCreated(user, reel)

        then:
        UserReelActivity.findByUserAndReelAndType(user, reel, ActivityType.CreateReel.value) != null
    }

    void "attempt to add real creation activity multiple times"() {
        given:
        activityService.reelCreated(user, reel)

        when:
        activityService.reelCreated(user, reel)

        then:
        def e = thrown(IllegalArgumentException)
        e.message == "Reel creation activity already exists for reel [${reel.id}]"
    }

    void "delete create reel activity"() {
        given:
        activityService.reelCreated(user, reel)

        when:
        activityService.reelDeleted(user, reel)

        then:
        UserReelActivity.findByUserAndReelAndType(user, reel, ActivityType.CreateReel.value) == null
    }

    @Unroll
    void "save join reel audience activity -- use uncategorized reel [#useUncategorizedReel]"() {
        given:
        def reelToUse = uncategorizedReel

        if(!useUncategorizedReel) {
            reelToUse = reel
            activityService.reelCreated(user, reel)
        }

        when:
        activityService.userJoinedAudience(user, reelToUse)

        then:
        UserReelActivity.findByUserAndReelAndType(user, reelToUse, ActivityType.JoinReelAudience.value) != null

        where:
        useUncategorizedReel << [true, false]
    }

    void "attempt to add join reel audience for reel with no create-reel activity"() {
        when:
        activityService.userJoinedAudience(user, reel)

        then:
        def e = thrown(IllegalArgumentException)
        e.message == "Create reel activity must exist before a join reel audience activity can be created for reel [${reel.id}]"
    }

    void "attempt to add join reel audience activity multiple times"() {
        given:
        activityService.reelCreated(user, reel)
        activityService.userJoinedAudience(user, reel)

        when:
        activityService.userJoinedAudience(user, reel)

        then:
        def e = thrown(IllegalArgumentException)
        e.message == "Join reel audience activity already exists for reel [${reel.id}]"
    }

    void "delete join reel audience activity"() {
        given:
        activityService.reelCreated(user, reel)
        activityService.userJoinedAudience(user, reel)

        when:
        activityService.userLeftAudience(user, reel)

        then:
        UserReelActivity.findByUserAndReelAndType(user, reel, ActivityType.JoinReelAudience.value) == null
    }

    @Unroll
    void "save video added to reel activity -- use uncategorized reel [#useUncategorizedReel]"() {
        given:
        def reelToUse = uncategorizedReel

        if(!useUncategorizedReel) {
            reelToUse = reel
            activityService.reelCreated(user, reel)
        }

        when:
        activityService.videoAddedToReel(user, reelToUse, video)

        then:
        UserReelActivity.findByUserAndReelAndType(user, reelToUse, ActivityType.AddVideoToReel.value) != null

        where:
        useUncategorizedReel << [true, false]
    }

    void "attempt to add video added to reel activity for reel with no create-reel activity"() {
        when:
        activityService.videoAddedToReel(user, reel, video)

        then:
        def e = thrown(IllegalArgumentException)
        e.message == "Create reel activity must exist before a video added to reel activity can be created for reel [${reel.id}]"
    }

    void "attempt to add video added to reel activity multiple times"() {
        given:
        activityService.reelCreated(user, reel)
        activityService.videoAddedToReel(user, reel, video)

        when:
        activityService.videoAddedToReel(user, reel, video)

        then:
        def e = thrown(IllegalArgumentException)
        e.message == "Video added to reel activity already exists for reel [${reel.id}]"
    }

    void "delete add video to reel activity"() {
        given:
        activityService.reelCreated(user, reel)
        activityService.videoAddedToReel(user, reel, video)

        when:
        activityService.videoRemovedFromReel(user, reel, video)

        then:
        UserReelActivity.findByUserAndReelAndType(user, reel, ActivityType.AddVideoToReel.value) == null
    }

    void "no user activities to delete"() {
        when:
        activityService.deleteAllUserActivity(user)

        then:
        notThrown(Exception)
    }

    void "delete all user activity"() {
        given:
        def activity1 = new UserReelActivity(user: user, reel: reel, type: ActivityType.CreateReel.value).save()
        def activity2 = new UserReelActivity(user: user, reel: reel, type: ActivityType.CreateReel.value).save()

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

    @Unroll
    void "activities exist but no users or reels specified -- users [#users], reels [#reels]"() {
        given:
        createActivityPage()

        when:
        def list = activityService.findActivities(users, reels)

        then:
        list.size() == 0

        where:
        users   |   reels
        null    |   null
        []      |   null
        null    |   []
        []      |   []
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

        assert list[0].type == ActivityType.AddVideoToReel.value
        assert list[1].type == ActivityType.CreateReel.value
    }

    void "create reel activity appears before others"() {
        given:
        def now = new Date()

        def createReel = new UserReelActivity(user: user, reel: reel, type: ActivityType.CreateReel.value)
        createReel.dateCreated = now
        createReel.save(flush: true)
        assert UserActivity.findByType(ActivityType.CreateReel.value) != null

        def userJoinedAudience = new UserReelActivity(user: user, reel: reel, type: ActivityType.JoinReelAudience.value)
        userJoinedAudience.dateCreated = now
        userJoinedAudience.save(flush: true)
        assert UserActivity.findByType(ActivityType.JoinReelAudience.value) != null

        def videoAddedToReel = new UserReelVideoActivity(user: user, reel: reel, video: video, type: ActivityType.AddVideoToReel.value)
        videoAddedToReel.dateCreated = now
        videoAddedToReel.save()
        assert UserActivity.findByType(ActivityType.AddVideoToReel.value) != null

        when:
        def activities = activityService.findActivities([user], [reel])

        then:
        activities.size() == 3

        and:
        activities[0] == videoAddedToReel
        activities[1] == userJoinedAudience
        activities[2] == createReel
    }

    @DirtiesRuntime
    void "list first page of activity if no page is specified"() {
        given:
        changeMaxActivitiesPerPage(TEST_MAX_ACTIVITIES_PER_PAGE)

        and:
        def secondPage = createActivityPage()
        def firstPage = createActivityPage()

        when:
        def list = activityService.findActivities([user], [reel])

        then:
        list.size() == TEST_MAX_ACTIVITIES_PER_PAGE

        and:
        list == firstPage
        list != secondPage
    }

    @DirtiesRuntime
    void "specify the page of activity to list"() {
        given:
        changeMaxActivitiesPerPage(TEST_MAX_ACTIVITIES_PER_PAGE)

        and:
        def thirdPage = createActivityPage()
        def secondPage = createActivityPage()
        def firstPage = createActivityPage()

        when:
        def list = activityService.findActivities([user], [reel], 2)

        then:
        list.size() == TEST_MAX_ACTIVITIES_PER_PAGE

        and:
        list != firstPage
        list == secondPage
        list != thirdPage
    }

    private List<UserReelActivity> createActivityPage() {
        List<UserReelActivity> activities = []
        TEST_MAX_ACTIVITIES_PER_PAGE.times {
            def video = VideoFactory.createVideo(user, "test $it")

            activities << new UserReelVideoActivity(user: user, reel: reel, video: video,
                    type: ActivityType.AddVideoToReel.value).save()

            sleep(2 * 1000)
        }
        activities.sort { a, b -> b.dateCreated <=> a.dateCreated }
        return activities
    }

    private void changeMaxActivitiesPerPage(final int max) {
        activityService.maxActivitiesPerPage = max
    }
}