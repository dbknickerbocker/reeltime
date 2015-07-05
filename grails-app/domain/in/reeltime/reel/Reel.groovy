package in.reeltime.reel

import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString
import in.reeltime.user.User
import in.reeltime.video.Video

@ToString(includeNames = true)
@EqualsAndHashCode(includes = ['name', 'owner'])
class Reel {

    static final UNCATEGORIZED_REEL_NAME = 'Uncategorized'

    static final MINIMUM_NAME_LENGTH = 5
    static final MAXIMUM_NAME_LENGTH = 25

    transient springSecurityService

    String name
    Date dateCreated

    static belongsTo = [owner: User]
    static hasOne = [audience: Audience]

    static transients = [
            'springSecurityService',
            'numberOfVideos',
            'numberOfAudienceMembers',
            'currentUserIsAnAudienceMember',
            'uncategorizedReel'
    ]

    static constraints = {
        name nullable: false, blank: false, minSize: MINIMUM_NAME_LENGTH, maxSize: MAXIMUM_NAME_LENGTH
        owner nullable: false
        audience unique: true
    }

    int getNumberOfVideos() {
        ReelVideo.countByReel(this)
    }

    int getNumberOfAudienceMembers() {
        audience?.members?.size() ?: 0
    }

    boolean getCurrentUserIsAnAudienceMember() {
        def currentUser = springSecurityService.currentUser as User
        audience?.members?.contains(currentUser)
    }

    boolean isUncategorizedReel() {
        name == UNCATEGORIZED_REEL_NAME
    }

    boolean containsVideo(Video video) {
        Reel.exists(this?.id) && Video.exists(video?.id) &&
                ReelVideo.findByReelAndVideo(this, video) != null

    }
}
