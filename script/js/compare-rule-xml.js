/**
 * A script to compare LanguageTool rule files and let the user know if any of
 * the supplied rules have been changed.
 *
 * Accepts rule definition files
 * (e.g. https://github.com/languagetool-org/languagetool/blob/master/languagetool-language-modules/en/src/main/resources/org/languagetool/rules/en/grammar.xml)
 * and a comma-separated list of rule names, which should be obtainable via
 * the Google Sheet or rule management service.
 *
 * Example output:
 *
 * Checking NO_SPACE_CLOSING_QUOTE ...
 * NO_SPACE_CLOSING_QUOTE is unchanged
 * Checking PUBIC_X ...
 * PUBIC_X is unchanged
 * Checking IN_PRINCIPAL ...
 * IN_PRINCIPAL is unchanged
 * Checking CURRENCY ...
 * CURRENCY is unchanged
 * Checking CURRENCY_SPACE ...
 * CURRENCY_SPACE has changed:
 * 4.3.xml:
 * <rule type="whitespace" id="CURRENCY_SPACE" name="Whitespace after currency symbols: '$ 100' ($100)">
 *             <pattern>
 *                 <token regexp="yes">[$€£¥\u8371]</token>
 *                 <token regexp="yes" spacebefore="yes">\d+</token>
 *             </pattern>
 *             <message>The currency mark is usually written without any whitespace: <suggestion>\1\2</suggestion>.</message>
 *             <short>Remove whitespace</short>
 *             <example correction="$100">You owe me <marker>$ 100</marker>.</example>
 *         </rule>
 * 5.3.xml:
 * <rule type="whitespace" id="CURRENCY_SPACE" name="Whitespace after currency symbols: '$ 100' ($100)">
 *             <antipattern>
 *                 <token regexp="yes">\d+</token>
 *                 <token regexp="yes">[$€£¥฿\u8371]</token>
 *                 <token regexp="yes">\d+</token>
 *             </antipattern>
 *             <pattern>
 *                 <token regexp="yes">[$€£¥฿\u8371]</token>
 *                 <token regexp="yes" spacebefore="yes">\d+</token>
 *             </pattern>
 *             <message>The currency mark is usually written without any whitespace.</message>
 *             <suggestion>\1\2</suggestion>
 *             <short>Remove whitespace</short>
 *             <example correction="$100">You owe me <marker>$ 100</marker>.</example>
 *         </rule>
 */

const fs = require("fs");
const libxmljs = require("libxmljs");

const [_, __, filePath1, filePath2, ruleStr] = process.argv;

if (!filePath1 || !filePath2 || !ruleStr) {
  console.error(
    "Incorrect arguments. Usage: node compare-rule-xml.js file-1.xml file2.xml RULE_1[,RULE_2,..].\nThe script received: ",
    { filePath1, filePath2, ruleStr }
  );
  process.exit(1);
}

const ruleIds = ruleStr.split(",");

const getFileStrFromPath = (filePath) => {
  try {
    return [filePath, fs.readFileSync(filePath, "utf-8")];
  } catch (e) {
    console.error(`Error reading ${filePath}: ${e.message}`);
    process.exit(1);
  }
};

/**
 * @returns {[string, libxmljs.Document]}
 */
const getXMLFromFile = ([filePath, str]) => {
  try {
    return [filePath, libxmljs.parseXml(str)];
  } catch (e) {}
  console.log(`Error parsing ${filePath}: ${e.message}`);
  process.exit(1);
};

/**
 * @param {libxmljs.Document} doc
 * @param {string} ruleId
 */
const getRuleNodeFromDoc = (doc, ruleId) => {
  const rulePredicate = `//rule[@id='${ruleId}']`;
  const ruleGroupPredicate = `//rulegroup[@id='${ruleId}']`;

  const rules = doc.find(rulePredicate) || [];
  const ruleGroups = doc.find(ruleGroupPredicate) || [];

  return {
    ruleId,
    rule: rules[0]?.toString() || ruleGroups[0]?.toString || undefined,
  };
};

const docs = [filePath1, filePath2].map(getFileStrFromPath).map(getXMLFromFile);

ruleIds.map((ruleId) => {
  console.log(`Checking ${ruleId} ...`);
  const [rulesIn1, rulesIn2] = docs.map(([path, doc]) => {
    return { path, ...getRuleNodeFromDoc(doc, ruleId) };
  });

  if (rulesIn2.rule && !rulesIn1.rule) {
    return console.log(`${ruleId} introduced in ${rulesIn2.path}`);
  }
  if (!rulesIn2.rule && rulesIn1.rule) {
    return console.log(`${ruleId} removed in ${rulesIn2.path}`);
  }
  if (!rulesIn2.rule && !rulesIn1.rule) {
    return console.log(`${ruleId} not found in either file`);
  }
  if (rulesIn2.rule.toString() !== rulesIn1.rule.toString()) {
    return console.log(
      `${ruleId} has changed: \n${rulesIn1.path}:\n${rulesIn1.rule.toString()}\n${
        rulesIn2.path
      }:\n${rulesIn2.rule.toString()}`
    );
  }
  return console.log(`${ruleId} is unchanged`);
});
