package net.fishbulb.jcod.namegen;

import java.util.Set;

public interface Namegen {

	/**
	 *  Parse a file with syllable sets.
	 * @param filename
	 */
	void parse (String filename);
	
	/**
	 * Generate a name.
	 * @param name The name of a known grammar 
	 * @return a new name
	 */
	String generate (String name);
	
	/**
	 *  Generate a name using a custom generation rule.
	 * @param name The name of a known grammar 
	 * @param rule The rule for generating the new name; checking the validity of the rule is not assured.
	 * @return a new name
	 */
	String generateCustom (String name, String rule);
	
	/**
	 *  Retrieve the list of all available syllable set names.
	 * @return A set of all known grammars
	 */
	Set<String> getAvailableGrammars ();
}