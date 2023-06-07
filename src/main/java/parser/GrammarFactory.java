/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package parser;

import utils.RegularExpression;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.lang3.StringUtils;
import utils.GrammarEntries;
import utils.GrammarEntryUnit;
import utils.UriLabel;

/**
 *
 * @author elahi 
 * creates a Grammar from output of Grammar Generation project.
 * serialised in some suitable format.
 */
public class GrammarFactory {

    private Grammar grammar = null;

    public GrammarFactory(File file,Boolean entityRetriveOnline,Integer numberOfEntities,String language) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            GrammarEntries grammarEntries = mapper.readValue(file, GrammarEntries.class);
            grammar = new Grammar(getAllGrammarRules(grammarEntries),entityRetriveOnline,numberOfEntities,language);
        } catch (IOException ex) {
            Logger.getLogger(GrammarFactory.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private List<GrammarRule> getAllGrammarRules(GrammarEntries grammarEntries) {
        List<GrammarRule> grammarRules = new ArrayList<GrammarRule>();
        for (GrammarEntryUnit grammarEntryUnit : grammarEntries.getGrammarEntries()) {
            List<String[]> questions = RegularExpression.ruleToRegEx(grammarEntryUnit.getSentences());
            if (!questions.isEmpty()&&grammarEntryUnit.getSparqlQuery() != null) {
                /*if(!grammarEntryUnit.getFrameType().equals("IPP")){
                    continue;
                }*/
                String sparql = this.modifySparql(grammarEntryUnit);
                GrammarRule grammarRule = new GrammarRule(questions, sparql,grammarEntryUnit.getBindingType());
                grammarRules.add(grammarRule);
            }

        }
        return grammarRules;
    }

   

    private String modifySparql(GrammarEntryUnit grammarEntryUnit) {
        String sparql = grammarEntryUnit.getSparqlQuery(), returnVariable = grammarEntryUnit.getReturnVariable();
        String frameType = grammarEntryUnit.getFrameType();
        String template = grammarEntryUnit.getSentenceTemplate();
        String property= this.findProperty(sparql);
        if (frameType.equals("NPP") || frameType.equals("VP") || frameType.equals("IPP")) {
            if (template != null && template.contains("HOW_MANY_THING")) {
                sparql = "SELECT COUNT(?Answer ) WHERE { ?subjOfProp " + "<" + property + ">" + " ?objOfProp .}";
            } else {
                sparql = "SELECT ?Answer WHERE { ?subjOfProp " + "<" + property + ">" + " ?objOfProp .}";
            }
            sparql=this.modifySparql(sparql, returnVariable);
        } else if (grammarEntryUnit.getFrameType().equals("AG")) {

            //System.out.println(" " + sparql + " " + grammarEntryUnit.getSentences());
            if (template.contains("adjectiveBaseForm")) {
                sparql = "SELECT ?Answer WHERE { ?subjOfProp " + "<" + property + ">" + " ?objOfProp .}";
                sparql=this.modifySparql(sparql, returnVariable);
            } else {
                sparql = grammarEntryUnit.getExecutable();
                sparql = sparql.replace("VARIABLE", "Arg").replace("subjOfProp", "Answer");
            }

        } else {
            sparql = "SELECT ?Answer WHERE { ?subjOfProp " + "<" + property + ">" + " ?objOfProp .}";
        }

        return sparql;
    }
    
    private String modifySparql(String sparql, String returnVariable) {
        if (returnVariable.contains("objOfProp")) {
            sparql = sparql.replace("subjOfProp", "Arg").replace("objOfProp", "Answer");
        } else {
            sparql = sparql.replace("objOfProp", "Arg").replace("subjOfProp", "Answer");
        }

        return sparql;
    }

    private String findProperty(String triple) {
        return StringUtils.substringBetween(triple, "<", ">");
    }

    public Grammar getGrammar() {
        return grammar;
    }

}
