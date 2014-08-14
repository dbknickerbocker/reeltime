package in.reeltime.reel

import in.reeltime.user.User

class Audience {

    static belongsTo = [reel: Reel]
    static hasMany = [members: User]

    static constraints = {
    }

    boolean hasMember(User user) {
        return members?.contains(user)
    }
}
