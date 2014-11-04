package in.reeltime.reel

import in.reeltime.user.User
import in.reeltime.video.Video

class Reel {

    static final UNCATEGORIZED_REEL_NAME = 'Uncategorized'

    static final MINIMUM_NAME_LENGTH = 5
    static final MAXIMUM_NAME_LENGTH = 25

    String name
    Date dateCreated

    static belongsTo = [owner: User]
    static hasOne = [audience: Audience]

    static transients = ['numberOfVideos', 'numberOfAudienceMembers']

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
}
