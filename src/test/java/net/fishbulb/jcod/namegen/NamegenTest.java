package net.fishbulb.jcod.namegen;

import java.util.Random;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.junit.Before;
import org.junit.Test;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl.LwjglFiles;

import toxi.math.MathUtils;

public class NamegenTest {

	@Before
	public void before() {
		Gdx.files = new LwjglFiles();
	}
	
	@Test
	public void testGrammar() {

		MathUtils.setDefaultRandomGenerator(new Random(1L)); // stabilize random generator

		String[] filenames = new String[]{
				"jice_celtic.cfg",
				"jice_fantasy.cfg",
				"jice_mesopotamian.cfg",
				"jice_norse.cfg",
				"jice_region.cfg",
				"jice_town.cfg",
				"mingos_demon.cfg",
				"mingos_dwarf.cfg",
				"mingos_norse.cfg",
				"mingos_standard.cfg",
				"mingos_town.cfg"
//				"test_grammar.cfg"
		};

		Stream.of(filenames).forEach(filename -> {
			
			Namegen ng = new NamegenImpl();
			ng.parse("namegen/" + filename);
			String grammar = ng.getAvailableGrammars().iterator().next();
			
			System.out.println( grammar + " : " );
			for (int i=0; i<10; i++) {
				String name = ng.generate(grammar);
				System.out.println(name);
			}

		});
	}
}