package in.reeltime.reel

import in.reeltime.user.User

class Audience {

    static belongsTo = [reel: Reel]
    static hasMany = [users: User]

    static constraints = {
    }
}