package models

import be.objectify.deadbolt.scala.{RoleGroups, allOf, allOfGroup, anyOf}

/**
  * The roles that exist for DexmoHQ
  *
  * @author Henrik Drefs
  */
object Roles {

  sealed trait Role extends be.objectify.deadbolt.scala.models.Role {
    def group: RoleGroups = allOfGroup(this.name)
  }

  object Superuser extends Role {
    override def name: String = "superuser"

    def withImplicits: List[Role] = List(this, Roles.UserAdmin)

  }

  object UserAdmin extends Role {
    override def name: String = "admin"

  }

  case class GameAdmin(forGameId: Int) extends Role {

    override def name: String = s"gameAdmin_$forGameId"

    override def group: RoleGroups = anyOf(allOf(this.name), allOf(Superuser.name))
  }

  object User extends Role {
    override def name: String = "user"

  }

}
