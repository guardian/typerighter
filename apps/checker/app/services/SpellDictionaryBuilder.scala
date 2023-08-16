package services

import java.io.BufferedWriter
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStreamWriter
import java.util
import java.util.Scanner

/* LanguageTool, a natural language style checker
 * Copyright (C) 2013 Daniel Naber (http://www.danielnaber.de)
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301
 * USA
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
