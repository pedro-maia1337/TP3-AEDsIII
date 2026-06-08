package auxiliares;

import auxiliares.ListaInvertida;
import auxiliares.ElementoLista;

import java.text.Normalizer;
import java.util.*;

public class IndiceCurso {

    // stop words
    private static final Set<String> STOP_WORDS = new HashSet<>(Arrays.asList(
            "a","ao","aos","as","ate","com","como","da","das","de","del","do","dos",
            "e","eh","em","entre","era","essa","esse","este","eu","foi","ha","isso",
            "isto","ja","la","lhe","lhes","lo","mas","me","mesmo","meu","meus","minha",
            "minhas","muito","na","nao","nas","nem","no","nos","nós","num","numa","o",
            "os","ou","para","pelo","pelos","pela","pelas","por","qual","quando","que",
            "quem","se","sem","seu","seus","sua","suas","também","te","teu","teus",
            "tua","tuas","um","uma","uns","umas","voce","vocês","vos",
            // preposições e artigos extras
            "ante","apos","ate","de","desde","durante","exceto","menos","salvo",
            "sob","sobre","tras",
            // numerais por extenso comuns
            "um","dois","tres","quatro","cinco","seis","sete","oito","nove","dez",
            "cem","mil"
    ));

    private final ListaInvertida listaInvertida;

    /**
     * @param tamanhoBlocos   tamanho de cada bloco na ListaInvertida (ex: 4)
     * @param arquivoDicionario  caminho do arquivo de dicionário (ex: ".\\dados\\cursos\\dict.listainv.db")
     * @param arquivoBlocos      caminho do arquivo de blocos    (ex: ".\\dados\\cursos\\blocos.listainv.db")
     */
    public IndiceCurso(int tamanhoBlocos, String arquivoDicionario, String arquivoBlocos)
            throws Exception {
        listaInvertida = new ListaInvertida(tamanhoBlocos, arquivoDicionario, arquivoBlocos);
    }

    //Indexa o nome de um curso recém-criado.
    public void indexarCurso(int idCurso, String nomeCurso) throws Exception {
        List<String> termos = extrairTermos(nomeCurso);
        Map<String, Integer> freq = contarFrequencias(termos);
        int totalTermos = termos.size();
        if (totalTermos == 0) return;

        for (Map.Entry<String, Integer> entrada : freq.entrySet()) {
            String termo    = entrada.getKey();
            float  tf       = (float) entrada.getValue() / totalTermos;
            listaInvertida.create(termo, new ElementoLista(idCurso, tf));
        }

        listaInvertida.incrementaEntidades();
    }


    //Remove a indexação de um curso excluído.
    public void removerIndexacao(int idCurso, String nomeCurso) throws Exception {
        List<String> termos = extrairTermos(nomeCurso);
        Set<String> termosUnicos = new HashSet<>(termos);

        for (String termo : termosUnicos) {
            listaInvertida.delete(termo, idCurso);
        }

        listaInvertida.decrementaEntidades();
    }

    // Atualiza a indexação quando o nome de um curso é alterado.
    public void atualizarIndexacao(int idCurso, String nomeAntigo, String nomeNovo)
            throws Exception {
        // Remove índice antigo (sem alterar contagem de entidades)
        List<String> termosAntigos = extrairTermos(nomeAntigo);
        Set<String> termosAntigosUnicos = new HashSet<>(termosAntigos);
        for (String t : termosAntigosUnicos) {
            listaInvertida.delete(t, idCurso);
        }

        // Insere novo índice (sem alterar contagem de entidades)
        List<String> termosNovos = extrairTermos(nomeNovo);
        Map<String, Integer> freq = contarFrequencias(termosNovos);
        int totalTermos = termosNovos.size();
        if (totalTermos == 0) return;

        for (Map.Entry<String, Integer> entrada : freq.entrySet()) {
            String termo = entrada.getKey();
            float  tf    = (float) entrada.getValue() / totalTermos;
            listaInvertida.create(termo, new ElementoLista(idCurso, tf));
        }
        // O número de entidades (cursos) não muda numa atualização de nome.
    }

    /**
     * Busca cursos pelos termos informados, aplicando TF×IDF.
     * @param consulta  texto digitado pelo usuário
     * @return  lista de IDs de cursos ordenada por pontuação decrescente
     */
    public List<Integer> buscar(String consulta) throws Exception {
        List<String> termosBusca = extrairTermos(consulta);
        if (termosBusca.isEmpty()) return new ArrayList<>();

        int N = listaInvertida.numeroEntidades();
        if (N == 0) return new ArrayList<>();

        // Acumula pontuações: idCurso → soma TF×IDF
        Map<Integer, Double> pontuacoes = new HashMap<>();

        for (String termo : new HashSet<>(termosBusca)) {   // sem repetir o mesmo termo
            ElementoLista[] elementos = listaInvertida.read(termo);
            if (elementos == null || elementos.length == 0) continue;

            // df = número de documentos que contêm o termo = tamanho da lista
            int    df  = elementos.length;
            double idf = Math.log10((double) N / df) + 1.0;

            for (ElementoLista el : elementos) {
                int    id = el.getId();
                double tf = el.getFrequencia();
                pontuacoes.merge(id, tf * idf, Double::sum);
            }
        }

        if (pontuacoes.isEmpty()) return new ArrayList<>();

        // Ordena por pontuação decrescente
        List<Map.Entry<Integer, Double>> lista = new ArrayList<>(pontuacoes.entrySet());
        lista.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));

        List<Integer> resultado = new ArrayList<>();
        for (Map.Entry<Integer, Double> entry : lista) {
            resultado.add(entry.getKey());
        }
        return resultado;
    }

    // auxiliares

    public static List<String> extrairTermos(String texto) {
        if (texto == null || texto.isEmpty()) return Collections.emptyList();

        // Normaliza: minúsculas, remove acentos
        String normalizado = removerAcentos(texto.toLowerCase(new Locale("pt", "BR")));
        String[] palavras  = normalizado.split("[^a-z0-9]+");

        List<String> termos = new ArrayList<>();
        for (String p : palavras) {
            if (p.isEmpty()) continue;
            if (STOP_WORDS.contains(p)) continue;
            termos.add(p);
        }
        return termos;
    }

    private static String removerAcentos(String s) {
        String normalizado = Normalizer.normalize(s, Normalizer.Form.NFD);
        return normalizado.replaceAll("\\p{InCombiningDiacriticalMarks}", "");
    }

    // Conta quantas vezes cada termo aparece na lista.
    private static Map<String, Integer> contarFrequencias(List<String> termos) {
        Map<String, Integer> freq = new LinkedHashMap<>();
        for (String t : termos) {
            freq.merge(t, 1, Integer::sum);
        }
        return freq;
    }
}