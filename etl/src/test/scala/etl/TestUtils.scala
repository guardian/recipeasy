package etl

import java.nio.file._

object TestUtils {

  def resourceToString(path: String) = new String(Files.readAllBytes(Paths.get(getClass.getClassLoader.getResource(path).toURI())))

}
