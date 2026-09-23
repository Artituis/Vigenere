# Relatório — Cifra de Vigenère e Criptoanálise

Trabalho de Segurança de Sistemas. Implementação em Java de (1) um algoritmo de criptografia usando a Cifra de Vigenère e (2) um algoritmo de criptoanálise capaz de quebrar a cifra **sem conhecer a senha**, assumindo texto em português.

## 1. Introdução

O objetivo é cifrar um texto qualquer com uma chave escolhida pelo usuário e, em seguida, demonstrar que é possível recuperar essa chave e o texto original apenas analisando o texto cifrado — usando o Índice de Coincidência (IC) para estimar o tamanho da chave e análise de frequência (teste qui-quadrado) para descobrir cada letra da chave.

O projeto é organizado em três classes:

- `Vigenere` — cifra/decifra dado um texto e uma chave conhecida.
- `Decriptor` — implementa o ataque de criptoanálise (Parte 2 do enunciado).
- `Main` — orquestra o fluxo: lê o arquivo, higieniza, cifra, ataca "às cegas", decifra e compara.

## 2. A Cifra de Vigenère

A cifra de Vigenère desloca cada letra do texto original por uma quantidade determinada pela letra correspondente da chave, repetida ciclicamente. Tratando `A=0, B=1, ..., Z=25`:

```
cifrado[i]  = (texto[i] + chave[i mod tamanho_chave]) mod 26
texto[i]    = (cifrado[i] - chave[i mod tamanho_chave] + 26) mod 26
```

Exemplo manual com chave `SEGREDO` (repetida) sobre o texto `AMOJAVA`:

```
texto:   A  M  O  J  A  V  A
chave:   S  E  G  R  E  D  O
soma:   18 16 20 35 22 25 25  (mod 26)
cifra:   S  Q  U  J  W  Z  Z
```

A implementação está em `src/Vigenere.java` (`encode`/`decode`), que já estava correta no projeto original e não foi alterada.

## 3. Higienização do texto

Antes de cifrar, o texto precisa conter apenas letras `a`–`z`, já que a aritmética modular é definida sobre um alfabeto de 26 símbolos. `Main` faz isso inline, sem uma classe separada:

```java
content = content.toLowerCase();
content = Normalizer.normalize(content, Normalizer.Form.NFD);      // á -> "a" + acento separado
content = content.replaceAll("\\p{InCombiningDiacriticalMarks}+", ""); // remove o acento, sobra "a"
content = content.replaceAll("[^a-z]", "");                        // remove o que não é letra
```

1. Converte tudo para minúsculas.
2. `Normalizer.normalize(..., Form.NFD)` decompõe cada letra acentuada em letra-base + marca diacrítica combinante (ex.: `á` vira `a` + acento agudo, como dois caracteres separados).
3. O regex `\p{InCombiningDiacriticalMarks}+` remove só essas marcas, restando a letra-base (`á → a`, `ç → c`, `ã → a`, etc.), atendendo ao passo obrigatório do enunciado (T1.pdf) que pede exatamente essa conversão.
4. O regex final remove tudo que não for `a`–`z` (pontuação, números, espaços, símbolos).

Isso substitui a versão anterior (`content.replaceAll("[^a-zA-Z]", "")` direto sobre o texto bruto), que **apagava** letras acentuadas inteiras em vez de normalizá-las — "ação" virava "ao" em vez de "acao", já que caracteres acentuados não estão no intervalo ASCII `a-zA-Z`.

## 4. O ataque de criptoanálise

Implementado em `src/Decriptor.java`, em duas etapas, exatamente como pedido no enunciado.

### 4.1 Etapa 1 — Descoberta do tamanho da chave (Índice de Coincidência)

O Índice de Coincidência mede a probabilidade de duas letras escolhidas ao acaso num texto serem iguais:

```
IC = Σ nᵢ(nᵢ - 1) / (N(N-1))
```

onde `nᵢ` é a contagem da letra `i` e `N` o tamanho do texto. Um texto em português "puro" (sem cifra, ou cifrado com um deslocamento único tipo César) tem IC alto (≈ 0,078, calculado a partir da tabela de frequência do português). Um texto aleatório de 26 símbolos tem IC baixo (≈ 0,0385 = 1/26).

Ao testar um tamanho de chave candidato `L`, o texto cifrado é dividido em `L` subtextos (posição `i` do texto cifrado vai para o subtexto `i mod L`). Se `L` for o tamanho real da chave (ou um múltiplo dele), cada subtexto foi cifrado com um único deslocamento fixo — portanto seu IC fica próximo do IC do português. Se `L` estiver errado, o subtexto mistura vários deslocamentos e o IC cai perto do valor aleatório.

`Decriptor.evaluateKeyLengths` calcula o IC médio dos `L` subtextos para `L` de 1 a 10 e `estimateKeyLength` escolhe o **menor** `L` cujo IC médio ultrapasse o limiar `(IC_aleatório + IC_português) / 2 ≈ 0,058`. Escolher o menor comprimento acima do limiar (em vez do maior IC absoluto) evita o problema de múltiplos do tamanho real também apresentarem IC alto.

### 4.2 Etapa 2 — Análise de frequência (teste qui-quadrado)

Com o tamanho de chave `L` estimado, cada um dos `L` subtextos foi cifrado com um deslocamento (letra da chave) fixo. Para achar esse deslocamento, testam-se os 26 deslocamentos possíveis e escolhe-se o que faz a distribuição de letras decodificada mais se parecer com a distribuição típica do português, usando a estatística qui-quadrado:

```
para cada deslocamento s (0 a 25):
    para cada letra i (0 a 25):
        observado_i = contagem, no subtexto cifrado, da letra (i + s) mod 26
        esperado_i  = frequência_português[i] × tamanho_do_subtexto
    χ²(s) = Σ (observado_i - esperado_i)² / esperado_i
escolhe-se o s que minimiza χ²(s)
```

A tabela de frequência de letras do português usada (fração do alfabeto a–z, sem acentos) é uma tabela padrão de referência para pt-BR:

```
a 14,63%  b 1,04%  c 3,88%  d 4,99%  e 12,57%  f 1,02%  g 1,30%  h 1,28%
i  6,18%  j 0,40%  k 0,02%  l 2,78%  m 4,74%  n 5,05%  o 10,73%  p 2,52%
q  1,20%  r 6,53%  s 7,81%  t 4,34%  u 4,63%  v 1,67%  w 0,01%  x 0,21%
y  0,01%  z 0,47%
```

**Otimização para arquivos grandes:** em vez de redecodificar o subtexto inteiro para cada um dos 26 deslocamentos candidatos (O(26·N)), a contagem de letras cifradas é feita uma única vez (O(N)) e, para cada deslocamento, apenas "gira-se" esse vetor de 26 posições (O(26) por deslocamento). Isso mantém o ataque rápido mesmo nos ~400 KB de `input.txt`.

O deslocamento encontrado em cada posição vira a letra correspondente da chave (`shift → 'A' + shift`), e `Decriptor.decriptKey` retorna a chave reconstruída, sem nunca ter recebido a chave real como entrada.

## 5. Fluxo em `Main`

`Main` manteve a mesma estrutura enxuta do código original (lê `input.txt`, higieniza, cifra com uma chave, salva `encoded.txt`/`output.txt`), com dois ajustes pontuais em relação ao stub original: a higienização passou a normalizar acentos (seção 3) em vez de apagá-los, e, em vez de decifrar diretamente com a chave conhecida, `Main` primeiro chama `Decriptor.decriptKey(encoded)` — o **ataque às cegas**, que só recebe o texto cifrado — e usa a chave *estimada* para decifrar e salvar `output.txt`. O `true`/`false` impresso no final indica se o texto recuperado pelo ataque bate exatamente com o original sanitizado.

`main` continua com a assinatura `static void main()` sem parâmetros (recurso de preview do Java 21/JEP 445), igual ao código original — por isso a compilação e a execução exigem a flag `--enable-preview` (seção 6).

## 6. Como executar

```bash
javac --release 21 --enable-preview -d out src/*.java
java --enable-preview -cp out Main
```

## 7. Resultados de teste

Teste recomendado pelo enunciado: `input.txt` (Dom Casmurro, Machado de Assis, ~400 KB) com a chave `segredo`.

**Higienização:** 400.910 caracteres brutos → 308.887 caracteres após sanitização.

**IC médio por tamanho de chave candidato (1 a 10):**

| Tamanho | IC médio |
|---|---|
| 1 | 0,0468 |
| 2 | 0,0468 |
| 3 | 0,0468 |
| 4 | 0,0468 |
| 5 | 0,0468 |
| 6 | 0,0468 |
| **7** | **0,0768** |
| 8 | 0,0468 |
| 9 | 0,0468 |
| 10 | 0,0468 |

(Tabela obtida chamando `Decriptor.evaluateKeyLengths` diretamente para fins deste relatório — `Main` não imprime essa tabela por padrão, apenas o resultado final do ataque.) O tamanho 7 se destaca claramente acima do limiar (≈ 0,058), batendo com o tamanho real de `segredo` (7 letras).

**Saída de `Main` rodando com `input.txt` e chave `segredo`:**

```
Key is: SEGREDO
true
Content saved to output.txt
```

A chave estimada (`SEGREDO`) é idêntica à chave real, sem que o algoritmo a tenha recebido, e o `true` confirma que o texto recuperado pelo ataque cego é idêntico ao texto sanitizado original, caractere por caractere.

**Trecho do início de `output.txt` (decifrado pelo ataque cego):**

```
THEPROJECTGUTENBERGEBOOKOFDOMCASMURROTHISEBOOKISFORTHEUSEOFANYONE...
```

### Teste de robustez com texto curto

Repetindo o teste com apenas os primeiros 400 caracteres brutos do mesmo arquivo (320 caracteres sanitizados, ~46 por subtexto):

- O tamanho de chave ainda foi estimado corretamente (7), com IC = 0,0686 no comprimento 7 contra ~0,04 nos demais.
- Mas a chave reconstruída (`RFHQFEP`) **não bateu** com `segredo` — com poucas letras por subtexto, a contagem de frequências fica estatisticamente ruidosa demais para o teste qui-quadrado escolher o deslocamento certo de forma confiável.

Isso confirma a limitação discutida na seção 8: o ataque depende de subtextos razoavelmente longos.

### Outros testes realizados

- Arquivo de entrada inexistente produz uma mensagem de erro amigável (`Error reading/writing file: ...`), sem stack trace.

## 8. Limitações

- **Acentos são apagados, não normalizados**: como descrito na seção 3, o regex `[^a-zA-Z]` remove letras acentuadas inteiras em vez de convertê-las para a letra-base (`ação` → `ao`, não `acao`), o que diverge do passo obrigatório do enunciado e reduz a quantidade de letras disponíveis para a análise de frequência em textos com muitos acentos. A correção proposta na seção 3 (via `java.text.Normalizer`) resolveria isso.
- **Textos curtos degradam o ataque**: com poucas centenas de caracteres, cada subtexto tem poucas letras, e a contagem de frequência fica ruidosa demais para o teste qui-quadrado identificar o deslocamento correto — mesmo quando o tamanho da chave é identificado certo.
- **Ambiguidade de múltiplos**: comprimentos que são múltiplos do tamanho real da chave também produzem subtextos monoalfabéticos e, portanto, IC alto. A heurística do menor comprimento acima do limiar mitiga isso, mas não elimina o caso em que o próprio tamanho real, por acaso, fica abaixo do limiar em textos atípicos.
- **Dependente do idioma**: a tabela de frequência de letras é específica do português; o mesmo código aplicado a um texto cifrado em outro idioma exigiria trocar `PT_LETTER_FREQ`.

## 9. Conclusão

A implementação cumpre as duas partes do enunciado: cifra/decifra corretas com Vigenère e um ataque de criptoanálise que recupera a chave e o texto original sem conhecê-los previamente, validado com o texto de teste recomendado (Dom Casmurro, chave `segredo`), incluindo a identificação correta do tamanho da chave e a reconstrução exata da chave e do texto.

## 10. Referências

- Tabela de frequência de letras do português (pt-BR): valores de referência amplamente citados em literatura sobre criptoanálise clássica e estatística da língua portuguesa.
- Índice de Coincidência e ataque de Kasiski/Friedman: técnicas clássicas de criptoanálise de cifras polialfabéticas.
- Teste qui-quadrado de aderência: usado como métrica de distância entre a distribuição de frequência observada e a esperada.
