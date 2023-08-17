package services.collins;

import org.jetbrains.annotations.Nullable;
import org.languagetool.GlobalConfig;
import org.languagetool.Language;
import org.languagetool.UserConfig;
import org.languagetool.language.BritishEnglish;
import org.languagetool.languagemodel.LanguageModel;
import org.languagetool.rules.Rule;
import org.languagetool.rules.spelling.SpellingCheckRule;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ResourceBundle;

public class CollinsEnglish extends BritishEnglish {
    @Override
    public SpellingCheckRule createDefaultSpellingRule(ResourceBundle messages) throws IOException {
        return new MorfologikCollinsSpellerRule(messages, this, null, Collections.emptyList());
    }

    @Override
    public List<Rule> getRelevantLanguageModelCapableRules(ResourceBundle messages, @Nullable LanguageModel lm, GlobalConfig globalConfig, UserConfig userConfig, Language motherTongue, List<Language> altLanguages) throws IOException {
        List<Rule> rules = new ArrayList<>(super.getRelevantLanguageModelCapableRules(messages, lm, globalConfig, userConfig, motherTongue, altLanguages));
        rules.add(new MorfologikCollinsSpellerRule(messages, this, globalConfig, userConfig, altLanguages, lm, motherTongue));
        return rules;
    }
}
