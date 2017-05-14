package forms

import play.api.data.Form
import play.api.data.Forms._

/**
 * The form which handles the sign up process.
 */
object SignUpForm {

  /**
   * A play framework form.
   */
  val form = Form(
    mapping(
      "nickname" -> nonEmptyText,
      "email" -> email,
      "password" -> nonEmptyText
    )(Data.apply)(Data.unapply)
  )

  /**
   * The form data.
   *
   * @param nickname The first name of a user.
   * @param email The email of the user.
   * @param password The password of the user.
   */
  case class Data(
    nickname: String,
    email: String,
    password: String)
}
