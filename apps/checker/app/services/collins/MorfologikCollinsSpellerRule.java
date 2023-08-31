package services.collins;

import java.io.IOException;
import java.util.List;
import java.util.ResourceBundle;

import org.languagetool.GlobalConfig;
import org.languagetool.Language;
import org.languagetool.UserConfig;
import org.languagetool.languagemodel.LanguageModel;
import org.languagetool.rules.en.AbstractEnglishSpellerRule;

public final class MorfologikCollinsSpellerRule extends AbstractEnglishSpellerRule {
    public static final String RULE_ID = "MORFOLOGIK_RULE_COLLINS";
    private static final String RESOURCE_FILENAME = "/resources/dictionary/collins.dict";

    public MorfologikCollinsSpellerRule(ResourceBundle messages, Language language, UserConfig userConfig, List<Language> altLanguages) throws IOException {
        super(messages, language, userConfig, altLanguages);
    }

    /**
     * @since 4.9
     */
    public MorfologikCollinsSpellerRule(ResourceBundle messages, Language language, GlobalConfig globalConfig, UserConfig userConfig, List<Language> altLanguages, LanguageModel languageModel, Language motherTongue) throws IOException {
        super(messages, language, globalConfig, userConfig, altLanguages, languageModel, motherTongue);
    }

    @Override
    public String getFileName() {
        return getClass().getResource(RESOURCE_FILENAME).getPath();
    }

    @Override
    public String getId() {
        return RULE_ID;
    }
}
