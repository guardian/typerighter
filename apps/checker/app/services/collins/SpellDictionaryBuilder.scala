package services.collins

import java.io._
import java.util
import java.util.Scanner

/* Scala conversion of Java original, derived from languagetool-tools: https://github.com/languagetool-org/languagetool/blob/master/languagetool-tools/src/main/java/org/languagetool/tools/SpellDictionaryBuilder.java
 * We have removed the CLI-oriented main method and added our buildDictionary method - hopefully we can
 * extend their version in some way rather than rolling our own.
 */

/** Create a Morfologik spelling binary dictionary from plain text data.
  */

class SpellDictionaryBuilder extends DictionaryBuilder {

  @throws[Exception]
  private def build(plainTextDictFile: File) = {
    var tempFile: File = null
    try {
      tempFile = tokenizeInput(plainTextDictFile)
      buildFSA(tempFile)
    } finally if (tempFile != null) tempFile.delete
  }

  def buildDictionary(wordList: List[String]): File = {
    val outputFileName = s"${getClass.getResource("/resources/dictionary").getPath}/collins.dict"
    setOutputFilename(outputFileName)
    val tempFile = File.createTempFile("word_list", ".txt")
    val bw = new BufferedWriter(
      new OutputStreamWriter(new FileOutputStream(tempFile.getAbsoluteFile), encoding)
    )
    try {
      wordList.foreach(word => bw.write(f"$word\n"))
      bw.close()
      val frequencyURL = getClass.getResource("/resources/dictionary/en_gb_wordlist.xml")
      val urlString = frequencyURL.getPath
      val frequencyFile = new File(urlString)
      readFreqList(frequencyFile)
      val inputFileWithFrequencyData = addFreqData(tempFile, true)
      build(inputFileWithFrequencyData)
    } catch {
      case e: IOException =>
        throw new RuntimeException(e)
    } finally {
      tempFile.delete()
    }
  }
  @throws[IOException]
  private def tokenizeInput(plainTextDictFile: File) = {
    val tempFile = File.createTempFile(classOf[SpellDictionaryBuilder].getSimpleName, ".txt")
    tempFile.deleteOnExit()
    val scanner = new Scanner(plainTextDictFile, encoding)
    try {
      val out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(tempFile), encoding))
      try
        while (scanner.hasNextLine) {
          val line = scanner.nextLine
          val sepPos =
            if (separator.isEmpty) -1
            else line.indexOf(separator)
          val occurrences =
            if (sepPos != -1) line.substring(sepPos + separator.length)
            else ""
          val lineWithoutOcc =
            if (sepPos != -1) line.substring(0, sepPos)
            else line
          val tokens = util.Arrays.asList(lineWithoutOcc)
          tokens.forEach(token => {
            if (token.nonEmpty) {
              out.write(token)
              if (sepPos != -1) {
                out.write(separator)
                if (tokens.size == 1) out.write(occurrences)
                else {
                  // TODO: as the word occurrence data from
                  // https://github.com/mozilla-b2g/gaia/tree/master/apps/keyboard/js/imes/latin/dictionaries
                  // has already been assigned in a previous step, we now cannot just use
                  // that value after having changed the tokenization...
                  out.write('A') // assume least frequent

                }
              }
              out.write('\n')
            }
          })
        }
      finally if (out != null) out.close()
    } finally if (scanner != null) scanner.close()

    tempFile
  }
}
