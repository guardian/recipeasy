package models

import com.gu.recipeasy.models._

object TagHelper {

  case class FormTags(
    cuisine: Seq[String],
    category: Seq[String],
    holiday: Seq[String],
    dietary: Seq[String]
  )

  object FormTags {
    def apply(tags: Tags): FormTags = {
      FormTags(
        cuisine = tags.list.collect { case t if t.category == "cuisines" => t.name },
        category = tags.list.collect { case t if t.category == "category" => t.name },
        holiday = tags.list.collect { case t if t.category == "holidays" => t.name },
        dietary = tags.list.collect { case t if t.category == "dietary" => t.name }
      )
    }
  }

  def getTags(tags: Seq[String], cat: String): Seq[Tag] = {
    tags.collect { case s: String if (!s.isEmpty) => Tag(s, cat) }
  }

}

