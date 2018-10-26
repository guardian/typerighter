import play.api._
import play.api.ApplicationLoader.Context
import play.filters.HttpFiltersComponents

class AppLoader extends ApplicationLoader {
  def load(context: Context) = {
    new AppComponents(context).application
  }
}