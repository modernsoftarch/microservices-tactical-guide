#!/usr/bin/env python3
"""Análisis de co-cambio entre módulos (capítulo 2, sección 2.4).

Cuenta cuántas veces dos módulos cambian en el mismo commit. Si dos módulos
cambian juntos la mayoría de las veces, separarlos en servicios distintos
crearía un monolito distribuido.

Uso (dentro de cualquier repositorio Git):

    python herramientas/cocambio.py --raiz src/main/java/com/miempresa/core/
    python herramientas/cocambio.py --raiz servicios/ --desde "6 months ago" --top 20

El módulo de un archivo es la primera carpeta debajo de --raiz. Por ejemplo,
con --raiz servicios/, el archivo servicios/emision/pom.xml pertenece a "emision".

Cómo leer la columna "acoplamiento" (cambios juntos / cambios del módulo menos activo):
  < 20 %   buen candidato a separarse
  20-50 %  revisa por qué cambian juntos; quizá un concepto está mal ubicado
  > 50 %   no los separes todavía: son el mismo contexto o la frontera está mal trazada

Solo usa la biblioteca estándar de Python 3.9+.
"""
from __future__ import annotations

import argparse
import subprocess
from collections import Counter
from itertools import combinations
from typing import Optional

SEPARADOR = "@@commit@@"


def modulo(ruta: str, raiz: str) -> Optional[str]:
    """Devuelve el módulo al que pertenece un archivo, o None si está fuera de la raíz."""
    if not ruta.startswith(raiz):
        return None
    resto = ruta[len(raiz):]
    return resto.split("/")[0] if "/" in resto else None


def leer_historial(desde: str) -> str:
    return subprocess.run(
        ["git", "log", f"--since={desde}", "--no-merges", "--name-only", f"--pretty=format:{SEPARADOR}"],
        capture_output=True, text=True, check=True,
    ).stdout


def contar(historial: str, raiz: str) -> tuple[Counter, Counter]:
    """Cuenta cambios por módulo y pares de módulos que cambiaron en el mismo commit."""
    cambios, pares = Counter(), Counter()
    for commit in historial.split(SEPARADOR):
        mods = {m for linea in commit.splitlines() if (m := modulo(linea.strip(), raiz))}
        cambios.update(mods)
        pares.update(combinations(sorted(mods), 2))
    return cambios, pares


def main() -> None:
    parser = argparse.ArgumentParser(description="Análisis de co-cambio entre módulos")
    parser.add_argument("--raiz", required=True, help="carpeta que contiene los módulos, con / al final")
    parser.add_argument("--desde", default="12 months ago", help='ventana de tiempo para git log (por defecto "12 months ago")')
    parser.add_argument("--top", type=int, default=15, help="cuántos pares mostrar")
    args = parser.parse_args()

    raiz = args.raiz if args.raiz.endswith("/") else args.raiz + "/"
    cambios, pares = contar(leer_historial(args.desde), raiz)

    if not pares:
        print("No hay commits que toquen dos módulos a la vez en esa ventana de tiempo.")
        return

    print(f"{'módulo A':<20}{'módulo B':<20}{'juntos':>7}{'acoplamiento':>14}")
    for (a, b), n in pares.most_common(args.top):
        acoplamiento = n / min(cambios[a], cambios[b])
        print(f"{a:<20}{b:<20}{n:>7}{acoplamiento:>13.0%}")


if __name__ == "__main__":
    main()
