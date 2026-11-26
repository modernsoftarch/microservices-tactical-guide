"""Pruebas de cocambio.py:  python -m unittest discover herramientas"""
import unittest

from cocambio import SEPARADOR, contar, modulo


class CocambioTest(unittest.TestCase):

    def test_modulo_es_la_primera_carpeta_bajo_la_raiz(self):
        self.assertEqual(modulo("servicios/emision/pom.xml", "servicios/"), "emision")
        self.assertIsNone(modulo("README.md", "servicios/"))
        self.assertIsNone(modulo("servicios/archivo-suelto.txt", "servicios/"))

    def test_cuenta_cambios_y_pares(self):
        historial = (
            f"{SEPARADOR}\nservicios/emision/A.java\nservicios/recaudo/B.java\n"
            f"{SEPARADOR}\nservicios/emision/C.java\n"
            f"{SEPARADOR}\nservicios/emision/D.java\nservicios/recaudo/E.java\nREADME.md\n"
        )
        cambios, pares = contar(historial, "servicios/")
        self.assertEqual(cambios["emision"], 3)
        self.assertEqual(cambios["recaudo"], 2)
        self.assertEqual(pares[("emision", "recaudo")], 2)


if __name__ == "__main__":
    unittest.main()
