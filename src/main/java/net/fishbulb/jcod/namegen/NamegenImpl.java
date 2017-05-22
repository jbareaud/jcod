package net.fishbulb.jcod.namegen;

import static net.fishbulb.jcod.util.extra.ExtraUtils.isEmpty;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.Value;
import toxi.math.MathUtils;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;

	
/**
 * An implementation for a grammar-driven name generator compatible with the .cfg grammar files of the libtcod project.  
 *
 * @see libtcod documentation at http://roguecentral.org/doryen/data/libtcod/doc/1.5.2/html2/namegen.html for an overview of the file structure.
 *
 * As some rules were ignored (pruning, triples...) there *will* be differences between libtcod namegen output 
 * and this implementation. Don't expect full equivalence.
 * 
 */
public class NamegenImpl implements Namegen {

	private Map<String, NGGrammar> grammars = new HashMap<>();
	
	public NamegenImpl() {	}
	
	@Override
	public void parse(String filename) {
		parseContent(checkFilename(filename).readString());
	}

	@Override
	public String generate(String name) {
		final NGGrammar grammar = retrieveGrammarOrThrowException(name);
		final String rule = pickRule(grammar);
		return innerGenerate(grammar, rule);
	}

	@Override
	public String generateCustom(String name, String rule) {
		final NGGrammar grammar = retrieveGrammarOrThrowException(name);
		return innerGenerate(grammar, rule);
	}
	
	@Override
	public Set<String> getAvailableGrammars() {
		return grammars.keySet();
	}

	private FileHandle checkFilename(String filename) {
		final FileHandle file = Gdx.files.internal(filename);
		if ( ! file.exists()) {
			new IllegalArgumentException("namegen file " + filename + "does not exist");
		}
		return file;
	}

	private void parseContent(final String content) {
		final Pattern pattern = Pattern.compile(
				"name\\s*\"(?<gname>.*?)\"\\s*\\{(?<grammar>.*?)\\}",
				Pattern.CASE_INSENSITIVE | Pattern.COMMENTS | Pattern.DOTALL | Pattern.MULTILINE);
		final Matcher matcher = pattern.matcher(content);
        while(matcher.find()) {
    		String grammar_name = matcher.group("gname");
    		String grammar = matcher.group("grammar");
        	parseGrammar(grammar_name, grammar);
        }
	}
	
	private void parseGrammar(final String grammar_name, final String grammar_str) {
		
		if (grammars.containsKey(grammar_name)) {
			throw new IllegalStateException("Trying to process an already existing grammar");
		}

		final NGGrammar grammar = new NGGrammar();
		final Pattern pattern = Pattern.compile("\\s*(?<listname>.*?)\\s*=.*?\"(?<listvalues>.*)\"");
		final Matcher matcher = pattern.matcher(grammar_str);

		while(matcher.find()) {
			String listname = matcher.group("listname");
			String listvalues = matcher.group("listvalues");
			switch(listname) {
			case "syllablesStart":
				grammar.getSyllables_start().addAll(extractList(listvalues)); break;
			case "syllablesMiddle"	:
				grammar.getSyllables_middle().addAll(extractList(listvalues));  break;
			case "syllablesEnd" :
				grammar.getSyllables_end().addAll(extractList(listvalues)); break;
			case "rules" :
				grammar.getRules().addAll(extractList(listvalues)); break;
			case "illegal" :
				grammar.getIllegal_strings().addAll(extractList(listvalues)); break;
			case "phonemesVocals" :
				grammar.getVocals().addAll(extractList(listvalues)); break;
			case "phonemesConsonants" :
				grammar.getConsonants().addAll(extractList(listvalues)); break;
			case "syllablesPre" :
				grammar.getSyllables_pre().addAll(extractList(listvalues)); break;
			case "syllablesPost" :
				grammar.getSyllables_post().addAll(extractList(listvalues)); break;
			default:
				throw new IllegalStateException("Rule not recognized : " + listname + " " + listvalues);
			}
		}
		grammars.put(grammar_name, grammar);
	}
	
	private String innerGenerate(final NGGrammar grammar, final String rule) {
		
		if (isEmpty(rule)) throw new IllegalArgumentException("rule cannot be null");
		
		String word = null;
		final StringBuilder sb = new StringBuilder();
		
		final Pattern pattern = Pattern.compile("\\$(?<percent>[0-9]*)?(?<token>P|s|m|e|p|v|c|\\?)(?<blank>_+)?");
		final Matcher matcher = pattern.matcher(rule);

		do {
			// reset
			sb.delete(0, sb.length());
			matcher.reset();
			
			while(matcher.find()) {
				String percent = matcher.group("percent");
				String token = matcher.group("token");
				String blank = matcher.group("blank");
				
				int chance = 100;
				if ( ! isEmpty(percent)) {
					chance = Integer.parseInt(percent);
				}
				
				if (MathUtils.random(100) <= chance) {
					switch (token) {
					case "P": sb.append(randomize(grammar.getSyllables_pre())); break;
	                case "s": sb.append(randomize(grammar.getSyllables_start())); break;
	                case "m": sb.append(randomize(grammar.getSyllables_middle())); break;
	                case "e": sb.append(randomize(grammar.getSyllables_end())); break;
	                case "p": sb.append(randomize(grammar.getSyllables_post())); break;
	                case "v": sb.append(randomize(grammar.getVocals())); break;
	                case "c": sb.append(randomize(grammar.getConsonants())); break;
	                case "?": 
	                	sb.append(
	                			MathUtils.random(1.0f) < 0.5
	                			? randomize(grammar.getVocals())
	                			: randomize(grammar.getConsonants())
	                	);
	                	break;
					default :
						throw new IllegalStateException("Malformed rule " + rule);
					}					
				}
				
				if (blank != null && blank.length() > 0) {
					sb.append(blank);
				}
			}
			// Finalizing
			deleteDoubleSpaces(sb);
			substituteWhitespace(sb);
			// trim
			while (sb.charAt(0) == ' ') {
				sb.delete(0, 1);
			}
			while (sb.charAt(sb.length() - 1 ) == ' ') {
				sb.delete(sb.length() -1, sb.length());
			}
			word = sb.toString();
		} while (isIllegal(grammar, word));
		return word;
	}

	private boolean isIllegal(final NGGrammar grammar, final String word) {
		return 
				isEmpty(word)
			||
				grammar.getIllegal_strings()
				.stream()
				.anyMatch((illegal) -> word.contains(illegal));
	}

	private List<String> extractList(String line) {
		return Stream.of(line.split(","))
			.map(String::trim)																	
			.collect(Collectors.toList());
	}

	private String randomize(List<String> list) {
		if (isEmpty(list)) return "";
		return list.get( MathUtils.random(list.size()) );
	}

	private NGGrammar retrieveGrammarOrThrowException(String name) {
		if ( ! grammars.containsKey(name)) throw new IllegalArgumentException("Grammar doesn't exist");
		return grammars.get(name);
	}

	private String pickRule(NGGrammar grammar) {

		if (isEmpty(grammar.getRules())) throw new IllegalStateException("Rules list is null or empty");
		
		String rule = null;
		int chance;
		do {
			chance = 100;
			rule = randomize(grammar.getRules());
			if (rule.startsWith("%")) {
				chance = Integer.parseInt(rule.substring(1, rule.indexOf("$")));
			}
		} while (MathUtils.random(100) > chance);
		return rule;
	}

	private void substituteWhitespace(final StringBuilder sb) {
		for (int index = 0; index < sb.length(); index++) {
		    if (sb.charAt(index) == '_') {
		        sb.setCharAt(index, ' ');
		    }
		}
	}

	private void deleteDoubleSpaces(final StringBuilder sb) {
		int index = 0;
		while ( (index = sb.indexOf("__")) != -1) {
			sb.deleteCharAt(index);
		}
	}
	
	@Value
	private class NGGrammar {
		  private List<String> vocals = new ArrayList<>();
		  private List<String> consonants = new ArrayList<>();
		  private List<String> syllables_pre = new ArrayList<>();
		  private List<String> syllables_start = new ArrayList<>();
		  private List<String> syllables_middle = new ArrayList<>();
		  private List<String> syllables_end = new ArrayList<>();
		  private List<String> syllables_post = new ArrayList<>();
		  private List<String> illegal_strings = new ArrayList<>();
		  private List<String> rules = new ArrayList<>();
	}
}