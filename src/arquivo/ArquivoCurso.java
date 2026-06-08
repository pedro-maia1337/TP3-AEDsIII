package arquivo;

import auxiliares.Arquivo;
import auxiliares.HashExtensivel;
import auxiliares.ArvoreBMais;
import auxiliares.IndiceCurso;
import auxiliares.ParUsuarioIdCursoId;
import auxiliares.ParNomeIdCurso;
import entidades.Curso;
import java.util.ArrayList;
import java.util.List;

public class ArquivoCurso extends Arquivo<Curso> {

    private HashExtensivel<CodigoToID>        indiceCodigo;
    private ArvoreBMais<ParUsuarioIdCursoId>  indiceUsuarioCurso;
    private ArvoreBMais<ParNomeIdCurso>       indiceNome;
    private IndiceCurso                       indiceInvertido;   // ← NOVO

    public ArquivoCurso() throws Exception {
        super("cursos", Curso.class.getConstructor());

        indiceCodigo = new HashExtensivel<>(
                CodigoToID.class.getConstructor(), 4,
                ".\\dados\\cursos\\indiceCodigo.d.db",
                ".\\dados\\cursos\\indiceCodigo.c.db");

        indiceUsuarioCurso = new ArvoreBMais<>(
                ParUsuarioIdCursoId.class.getConstructor(), 5,
                ".\\dados\\cursos\\indiceUsuarioCurso.db");

        indiceNome = new ArvoreBMais<>(
                ParNomeIdCurso.class.getConstructor(), 5,
                ".\\dados\\cursos\\indiceNome.db");

        // Índice invertido (lista invertida) para busca por palavras-chave
        indiceInvertido = new IndiceCurso(
                4,
                ".\\dados\\cursos\\dict.listainv.db",
                ".\\dados\\cursos\\blocos.listainv.db");
    }


    @Override
    public int create(Curso c) throws Exception {
        // Verifica se já existe um curso com o mesmo código compartilhável
        if (readByCodigo(c.getCodigoCompartilhavel()) != null) {
            throw new Exception("Código compartilhável já existe: " + c.getCodigoCompartilhavel());
        }

        int id = super.create(c);

        indiceCodigo.create(new CodigoToID(c.getCodigoCompartilhavel(), id));
        indiceUsuarioCurso.create(new ParUsuarioIdCursoId(c.getIdUsuario(), id));
        indiceNome.create(new ParNomeIdCurso(c.getNome(), id));

        indiceInvertido.indexarCurso(id, c.getNome());

        return id;
    }


    public Curso readByCodigo(String codigo) throws Exception {
        CodigoToID par = indiceCodigo.read(CodigoToID.hash(codigo));
        if (par == null) return null;
        return super.read(par.getId());
    }


    @Override
    public boolean delete(int id) throws Exception {
        Curso c = super.read(id);
        if (c == null) return false;

        if (super.delete(id)) {
            indiceCodigo.delete(CodigoToID.hash(c.getCodigoCompartilhavel()));
            indiceUsuarioCurso.delete(new ParUsuarioIdCursoId(c.getIdUsuario(), id));
            indiceNome.delete(new ParNomeIdCurso(c.getNome(), id));

            // Remove do índice invertido
            indiceInvertido.removerIndexacao(id, c.getNome());

            return true;
        }
        return false;
    }


    @Override
    public boolean update(Curso novo) throws Exception {
        Curso antigo = super.read(novo.getId());
        if (antigo == null) return false;

        boolean codigoMudou = !novo.getCodigoCompartilhavel()
                .equals(antigo.getCodigoCompartilhavel());
        if (codigoMudou && readByCodigo(novo.getCodigoCompartilhavel()) != null) {
            throw new Exception("Código compartilhável já em uso: "
                    + novo.getCodigoCompartilhavel());
        }

        if (super.update(novo)) {
            if (codigoMudou) {
                indiceCodigo.delete(CodigoToID.hash(antigo.getCodigoCompartilhavel()));
                indiceCodigo.create(new CodigoToID(novo.getCodigoCompartilhavel(), novo.getId()));
            }

            if (novo.getIdUsuario() != antigo.getIdUsuario()) {
                indiceUsuarioCurso.delete(
                        new ParUsuarioIdCursoId(antigo.getIdUsuario(), novo.getId()));
                indiceUsuarioCurso.create(
                        new ParUsuarioIdCursoId(novo.getIdUsuario(), novo.getId()));
            }

            boolean nomeMudou = !novo.getNome().equals(antigo.getNome());
            if (nomeMudou) {
                indiceNome.delete(new ParNomeIdCurso(antigo.getNome(), novo.getId()));
                indiceNome.create(new ParNomeIdCurso(novo.getNome(), novo.getId()));

                indiceInvertido.atualizarIndexacao(novo.getId(), antigo.getNome(), novo.getNome());
            }

            return true;
        }
        return false;
    }


    // Busca cursos de um usuário específico
    public ArrayList<Curso> readByUsuario(int idUsuario) throws Exception {
        ArrayList<Curso> cursos = new ArrayList<>();

        // Adiciona também uma busca sem especificar o ID do curso para pegar todos
        ArrayList<ParUsuarioIdCursoId> todosPares = indiceUsuarioCurso.read(null);
        for (ParUsuarioIdCursoId par : todosPares) {
            if (par.getIdUsuario() == idUsuario) {
                Curso curso = super.read(par.getIdCurso());
                if (curso != null && curso.getIdUsuario() == idUsuario) {
                    cursos.add(curso);
                }
            }
        }
        return cursos;
    }

    // Busca cursos ordenados por nome
    public ArrayList<Curso> readAll() throws Exception {
        ArrayList<Curso> cursos = new ArrayList<>();

        // Busca todos os cursos via índice de nome
        ArrayList<ParNomeIdCurso> pares = indiceNome.read(null);
        for (ParNomeIdCurso par : pares) {
            Curso curso = super.read(par.getIdCurso());
            if (curso != null) {
                cursos.add(curso);
            }
        }
        return cursos;
    }

    //Busca cursos por palavras-chave usando TF×IDF.

    public ArrayList<Curso> buscarPorPalavras(String consulta) throws Exception {
        List<Integer> ids = indiceInvertido.buscar(consulta);
        ArrayList<Curso> resultado = new ArrayList<>();
        for (int id : ids) {
            Curso c = super.read(id);
            if (c != null) {
                resultado.add(c);
            }
        }
        return resultado;
    }
}