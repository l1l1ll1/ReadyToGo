package kr.ac.baekseok.readytogo.ui.format

import org.junit.Assert.assertEquals
import org.junit.Test

class KoreanParticleTest {

    @Test
    fun `한글 받침에 따라 조사가 갈린다`() {
        assertEquals("라멘 하루카와", withParticle("라멘 하루카", Particle.AND))
        assertEquals("기후 라멘이", withParticle("기후 라멘", Particle.SUBJECT))
        assertEquals("니조시장은", withParticle("니조시장", Particle.TOPIC))
        assertEquals("오도리 공원을", withParticle("오도리 공원", Particle.OBJECT))
        assertEquals("스미레 미소라멘과", withParticle("스미레 미소라멘", Particle.AND))
        assertEquals("시계탑을", withParticle("시계탑", Particle.OBJECT))
        assertEquals("카페는", withParticle("카페", Particle.TOPIC))
    }

    @Test
    fun `끝의 괄호와 기호는 건너뛴다`() {
        assertEquals("수프카레 스아게+가", withParticle("수프카레 스아게+", Particle.SUBJECT))
        assertEquals("돈키호테 (본점)이", withParticle("돈키호테 (본점)", Particle.SUBJECT))
    }

    @Test
    fun `숫자와 영문은 읽는 소리로 어림한다`() {
        assertEquals("Toriton Kita 8이", withParticle("Toriton Kita 8", Particle.SUBJECT))
        assertEquals("Espresso D'Works가", withParticle("Espresso D'Works", Particle.SUBJECT))
        assertEquals("Parfait Shop NANAKAMADOU는", withParticle("Parfait Shop NANAKAMADOU", Particle.TOPIC))
        assertEquals("Sapporo Station과", withParticle("Sapporo Station", Particle.AND))
    }
}
