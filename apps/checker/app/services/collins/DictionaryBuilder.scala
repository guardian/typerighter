package services.collins

import morfologik.tools.{DictCompile, FSACompile, SerializationFormat}
import org.languagetool.JLanguageTool

import java.io._
import java.nio.charset.StandardCharsets
import java.nio.file.{Files, StandardCopyOption}
import java.util
import java.util.regex.Pattern
import java.util.{Collections, Scanner}

/* Scala conversion of Java original, derived from languagetool-tools: https://github.com/languagetool-org/languagetool/blob/master/languagetool-tools/src/main/java/org/languagetool/tools/DictionaryBuilder.java
 * Hopefully we can publish from that repository and remove this version entirely.
 */

/** Create a Morfologik binary dictionary from plain text data.
  */
object DictionaryBuilder {
  private val FREQ_RANGES_IN = 256
  private val FREQ_RANGES_OUT = 26 // (A-Z)

  private val FIRST_RANGE_CODE = 65 // character 'A', less frequent words

  private val serializationFormat = SerializationFormat.CFSA2
}

class DictionaryBuilder() {
  final private val freqList = new util.HashMap[String, Integer]
  final private val pFreqEntry =
    Pattern.compile(".*<w f=\"(\\d+)\"(?: flags=\"(.*?)\")?>(.+)</w>.*")
  // Valid for tagger dictionaries (wordform_TAB_lemma_TAB_postag) or spelling dictionaries (wordform)
  final private val pTaggerEntry = Pattern.compile("^([^\t]+).*$")
  private var outputFilename: String = null
  protected val separator = "+"
  protected val encoding = "iso-8859-1"
  protected def setOutputFilename(outputFilename: String): Unit = {
    this.outputFilename = outputFilename
  }

  protected def getOutputFilename = outputFilename

  @throws[Exception]
  protected def buildDict(inputFile: File) = {
    val outputFile = new File(outputFilename)
    val resultFile = new File(
      inputFile.toString.replaceAll("\\.txt$", JLanguageTool.DICTIONARY_FILENAME_EXTENSION)
    )
    val buildOptions = List(
      "--exit",
      "false",
      "-i",
      inputFile.toString,
      "-f",
      DictionaryBuilder.serializationFormat.toString
    )
    System.out.println(
      "Running Morfologik DictCompile.main with these options: " + (buildOptions.toString())
    )
    DictCompile.main(buildOptions.toArray)
    // move output file to the desired path and name
    Files.move(resultFile.toPath, outputFile.toPath, StandardCopyOption.REPLACE_EXISTING)
    System.out.println(
      "Done. The binary dictionary has been written to " + outputFile.getAbsolutePath
    )
    outputFile
  }

  @throws[Exception]
  protected def buildFSA(inputFile: File) = {
    val resultFile = new File(outputFilename)
    val buildOptions = List(
      "--exit",
      "false",
      "-i",
      inputFile.toString,
      "-o",
      resultFile.toString,
      "-f",
      DictionaryBuilder.serializationFormat.toString
    )
    System.out.println(
      "Running Morfologik FSACompile.main with these options: " + (buildOptions.toString())
    )
    FSACompile.main(buildOptions.toArray)
    System.out.println(
      "Done. The binary dictionary has been written to " + resultFile.getAbsolutePath
    )
    resultFile
  }

  protected def readFreqList(freqListFile: File): Unit = {
    val fis = new FileInputStream(freqListFile.getPath)
    val reader = new InputStreamReader(fis, StandardCharsets.UTF_8)
    val br = new BufferedReader(reader)
    try {
      var line: String = null
      line = br.readLine()
      while (line != null) {
        val m = pFreqEntry.matcher(line)
        if (m.matches) freqList.put(m.group(3), m.group(1).toInt)
        line = br.readLine()
      }
    } catch {
      case e: IOException =>
        throw new RuntimeException("Cannot read file: " + freqListFile.getAbsolutePath)
    } finally {
      if (fis != null) fis.close()
      if (reader != null) reader.close()
      if (br != null) br.close()
    }

  }

  @throws[IOException]
  protected def addFreqData(dictFile: File, useSeparator: Boolean) = {

    val tempFile =
      File.createTempFile(classOf[DictionaryBuilder].getSimpleName, "WithFrequencies.txt")
    tempFile.deleteOnExit()

    var freqValuesApplied = 0
    val bw = new BufferedWriter(
      new OutputStreamWriter(new FileOutputStream(tempFile.getAbsoluteFile), encoding)
    )
    val br = new BufferedReader(
      new InputStreamReader(new FileInputStream(dictFile.getAbsoluteFile), encoding)
    )
    try {
      var line: String = null
      val maxFreq = Collections.max(freqList.values)
      val maxFreqLog = Math.log(maxFreq.toDouble)
      line = br.readLine()
      while (line != null) {
        val m = pTaggerEntry.matcher(line)
        if (m.matches) {
          var freq = 0
          val key = m.group(1)
          if (freqList.containsKey(key)) {
            freq = freqList.get(key)
            freqValuesApplied += 1
          }
          var normalizedFreq = freq
          if (freq > 0 && maxFreq > 255) {
            val freqZeroToOne = Math.log(freq) / maxFreqLog // spread number better over the range

            normalizedFreq =
              (freqZeroToOne * (DictionaryBuilder.FREQ_RANGES_IN - 1)).toInt // 0 to 255

          }
          if (normalizedFreq < 0 || normalizedFreq > 255)
            throw new RuntimeException(
              "Frequency out of range (0-255): " + normalizedFreq + " in word " + key
            )
          // Convert integers 0-255 to ranges A-Z, and write output
          val freqChar = Character.toString(
            (DictionaryBuilder.FIRST_RANGE_CODE + normalizedFreq * DictionaryBuilder.FREQ_RANGES_OUT / DictionaryBuilder.FREQ_RANGES_IN).toChar
          )
          // add separator only in speller dictionaries
          if (useSeparator) bw.write(line + separator + freqChar + "\n")
          else bw.write(line + freqChar + "\n")
        }
        line = br.readLine()
      }
      System.out.println(
        s"${freqList.size} frequency values applied to $freqValuesApplied word forms."
      )
    } catch {
      case e: IOException =>
        throw new RuntimeException("Cannot read file: " + dictFile.getAbsolutePath)
    } finally {
      if (bw != null) bw.close()
      if (br != null) br.close()
    }

    tempFile
  }

  @throws[RuntimeException]
  @throws[IOException]
  protected def convertTabToSeparator(inputFile: File) = {
    val outputFile =
      File.createTempFile(classOf[DictionaryBuilder].getSimpleName + "_separator", ".txt")
    outputFile.deleteOnExit()
    val scanner = new Scanner(inputFile, encoding)
    val out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputFile), encoding))
    try
      while (scanner.hasNextLine) {
        val line = scanner.nextLine
        val parts = line.split("\t")
        if (parts.length == 3) {
          out.write(parts(1) + separator + parts(0) + separator + parts(2))
          out.write('\n')
        } else
          System.err.println(
            "Invalid input, expected three tab-separated columns in " + inputFile + ": " + line + " => ignoring"
          )
      }
    finally {
      if (scanner != null) scanner.close()
      if (out != null) out.close()
    }

    outputFile
  }

}
