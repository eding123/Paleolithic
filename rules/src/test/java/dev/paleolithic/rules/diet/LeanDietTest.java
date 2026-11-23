package dev.paleolithic.rules.diet;

import org.junit.Test;

import static org.junit.Assert.*;

public class LeanDietTest {

    @Test
    public void sixLeanMealsStartTheLeanDiet() {
        LeanDiet d = new LeanDiet();
        for (int i = 0; i < 5; i++) d.eat(LeanDiet.Tag.LEAN, 8, 0.8f);
        assertFalse(d.leanDiet());
        d.eat(LeanDiet.Tag.LEAN, 8, 0.8f);
        assertTrue(d.leanDiet());
        LeanDiet.Meal next = d.eat(LeanDiet.Tag.LEAN, 8, 0.8f);
        assertEquals(8, next.nutrition());
        assertEquals(0.4f, next.saturationModifier(), 1e-6);
    }

    @Test
    public void severeLeanDietCutsHungerTo75Percent() {
        LeanDiet d = new LeanDiet();
        d.load(10);
        LeanDiet.Meal m = d.eat(LeanDiet.Tag.LEAN, 8, 0.8f);
        assertEquals(6, m.nutrition());
        assertEquals(0.4f, m.saturationModifier(), 1e-6);
    }

    @Test
    public void fattyOrPlantFoodLowersTheCountBy3() {
        LeanDiet d = new LeanDiet();
        d.load(7);
        d.eat(LeanDiet.Tag.FATTY, 8, 0.8f);
        assertEquals(4, d.leanCount());
        d.eat(LeanDiet.Tag.PLANT, 2, 0.1f);
        assertEquals(1, d.leanCount());
        d.eat(LeanDiet.Tag.PLANT, 2, 0.1f);
        assertEquals(0, d.leanCount());
    }

    @Test
    public void nutritionNeverDropsBelowOne() {
        LeanDiet d = new LeanDiet();
        d.load(12);
        assertEquals(1, d.eat(LeanDiet.Tag.LEAN, 1, 0.1f).nutrition());
    }
}
