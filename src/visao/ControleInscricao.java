package visao;

import arquivo.ArquivoCurso;
import arquivo.ArquivoInscricao;
import arquivo.ArquivoUsuario;
import auxiliares.Teclado;
import entidades.Curso;
import entidades.Inscricao;
import entidades.Usuario;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;

public class ControleInscricao {

    private ArquivoCurso arquivoCurso;
    private ArquivoInscricao arquivoInscricao;
    private ArquivoUsuario arquivoUsuario;
    private VisaoCurso visao;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final int ITENS_POR_PAGINA = 10;

    public ControleInscricao() throws Exception {
        arquivoCurso     = new ArquivoCurso();
        arquivoInscricao = new ArquivoInscricao();
        arquivoUsuario   = new ArquivoUsuario();
        visao            = new VisaoCurso();
    }

    // Menu Principal

    public void menu(Usuario usuarioLogado) {
        String opcao;
        do {
            try {
                ArrayList<Inscricao> minhasInscricoes =
                        arquivoInscricao.readByUsuario(usuarioLogado.getId());
                exibirMenu(usuarioLogado, minhasInscricoes);
                opcao = Teclado.lerLinha().trim().toUpperCase();

                switch (opcao) {
                    case "A":
                        buscarPorCodigo(usuarioLogado);
                        break;
                    case "B":
                        buscarPorPalavrasChave(usuarioLogado);   // ← implementado
                        break;
                    case "C":
                        listarTodosCursos(usuarioLogado);
                        break;
                    case "R":
                        return;
                    default:
                        try {
                            int num = Integer.parseInt(opcao);
                            if (num >= 1 && num <= minhasInscricoes.size()) {
                                telaGerenciarInscricao(
                                        minhasInscricoes.get(num - 1), usuarioLogado);
                            } else {
                                System.out.println("Opção inválida!");
                            }
                        } catch (NumberFormatException e) {
                            System.out.println("Opção inválida!");
                        }
                        break;
                }
            } catch (Exception e) {
                visao.mensagemErro("Erro do sistema: " + e.getMessage());
            }
        } while (true);
    }

    private void exibirMenu(Usuario usuarioLogado, ArrayList<Inscricao> inscricoes) {
        System.out.println("\n\nEntrePares 1.0");
        System.out.println("--------------");
        System.out.println("> Início > Minhas inscrições");
        System.out.println("\nINSCRIÇÕES");

        if (inscricoes.isEmpty()) {
            System.out.println("Você não possui inscrições ativas.");
        } else {
            for (int i = 0; i < inscricoes.size(); i++) {
                Inscricao insc = inscricoes.get(i);
                try {
                    Curso curso = arquivoCurso.read(insc.getIdCurso());
                    if (curso == null) continue;
                    System.out.printf("(%d) %s - %s%s%n",
                            i + 1,
                            curso.getNome(),
                            curso.getDataInicio().format(FMT),
                            getLabelStatus(curso));
                } catch (Exception e) { /* ignora curso removido */ }
            }
        }

        System.out.println("\n(A) Buscar curso por código");
        System.out.println("(B) Buscar curso por palavras-chave");
        System.out.println("(C) Listar todos os cursos");
        System.out.println("\n(R) Retornar ao menu anterior");
        System.out.print("\nOpção: ");
    }

    // Busca por código

    private void buscarPorCodigo(Usuario usuarioLogado) {
        System.out.println("\nBusca por código");
        System.out.println("================");
        System.out.print("Informe o código do curso (vazio para cancelar): ");
        String codigo = Teclado.lerLinha().trim();
        if (codigo.isEmpty()) {
            System.out.println("Operação cancelada.");
            return;
        }

        try {
            Curso curso = arquivoCurso.readByCodigo(codigo);
            if (curso == null) {
                System.out.println("Nenhum curso encontrado com o código: " + codigo);
                return;
            }
            telaDetalheCurso(curso, usuarioLogado,
                    "> Início > Minhas inscrições > " + curso.getNome());
        } catch (Exception e) {
            visao.mensagemErro("Erro ao buscar curso: " + e.getMessage());
        }
    }

    // Busca por palavra chave

    //Pede as palavras-chave ao usuário, consulta o índice invertido e exibe
    private void buscarPorPalavrasChave(Usuario usuarioLogado) {
        System.out.println("\nBusca por palavra-chave");
        System.out.println("-----------------------");
        System.out.print("Digite as palavras de busca (vazio para cancelar): ");
        String consulta = Teclado.lerLinha().trim();
        if (consulta.isEmpty()) {
            System.out.println("Operação cancelada.");
            return;
        }

        try {
            ArrayList<Curso> resultados = arquivoCurso.buscarPorPalavras(consulta);

            if (resultados.isEmpty()) {
                System.out.println("\nNenhum curso encontrado para: \"" + consulta + "\"");
                return;
            }

            // Exibe paginado (10 por página)
            int totalPaginas = (int) Math.ceil((double) resultados.size() / ITENS_POR_PAGINA);
            int paginaAtual  = 1;

            String opcao;
            do {
                exibirPaginaResultados(resultados, paginaAtual, totalPaginas, consulta);
                opcao = Teclado.lerLinha().trim().toUpperCase();

                int inicio      = (paginaAtual - 1) * ITENS_POR_PAGINA;
                int fim         = Math.min(inicio + ITENS_POR_PAGINA, resultados.size());
                int qtdNaPagina = fim - inicio;

                if (opcao.equals("A")) {
                    if (paginaAtual > 1) paginaAtual--;
                    else System.out.println("Você já está na primeira página.");
                } else if (opcao.equals("B")) {
                    if (paginaAtual < totalPaginas) paginaAtual++;
                    else System.out.println("Você já está na última página.");
                } else if (opcao.equals("R")) {
                    return;
                } else {
                    try {
                        int num = Integer.parseInt(opcao);
                        // (1)-(9) → índices 0-8; (0) → índice 9
                        int idxNaPagina = (num == 0) ? 9 : num - 1;
                        if (idxNaPagina >= 0 && idxNaPagina < qtdNaPagina) {
                            Curso selecionado = resultados.get(inicio + idxNaPagina);
                            telaDetalheCurso(selecionado, usuarioLogado,
                                    "> Início > Minhas inscrições > Busca > "
                                            + selecionado.getNome());
                        } else {
                            System.out.println("Opção inválida!");
                        }
                    } catch (NumberFormatException e) {
                        System.out.println("Opção inválida!");
                    }
                }
            } while (true);

        } catch (Exception e) {
            visao.mensagemErro("Erro na busca: " + e.getMessage());
        }
    }

    private void exibirPaginaResultados(ArrayList<Curso> cursos, int paginaAtual,
                                        int totalPaginas, String consulta) {
        System.out.println("\n\nEntrePares 1.0");
        System.out.println("--------------");
        System.out.println("> Início > Minhas inscrições > Busca: \"" + consulta + "\"");
        System.out.printf("%nPágina %d de %d — %d resultado(s)%n%n",
                paginaAtual, totalPaginas, cursos.size());

        int inicio = (paginaAtual - 1) * ITENS_POR_PAGINA;
        int fim    = Math.min(inicio + ITENS_POR_PAGINA, cursos.size());

        for (int i = inicio; i < fim; i++) {
            Curso curso = cursos.get(i);
            int numeroExibido = (i - inicio + 1) % ITENS_POR_PAGINA;   // 1-9 depois 0
            System.out.printf("(%d) %s - %s%s%n",
                    numeroExibido,
                    curso.getNome(),
                    curso.getDataInicio().format(FMT),
                    getLabelStatus(curso));
        }

        System.out.println();
        if (paginaAtual > 1)            System.out.println("(A) Página anterior");
        if (paginaAtual < totalPaginas) System.out.println("(B) Próxima página");
        System.out.println("\n(R) Retornar ao menu anterior");
        System.out.print("\nOpção: ");
    }

    // Listagem

    private void listarTodosCursos(Usuario usuarioLogado) {
        try {
            ArrayList<Curso> todos = arquivoCurso.readAll();
            todos.sort(Comparator.comparing(Curso::getDataInicio));

            // Remove cursos do próprio usuário
            ArrayList<Curso> disponiveis = new ArrayList<>();
            for (Curso c : todos) {
                if (c.getIdUsuario() != usuarioLogado.getId()) {
                    disponiveis.add(c);
                }
            }

            if (disponiveis.isEmpty()) {
                System.out.println("\nNenhum curso disponível no momento.");
                return;
            }

            int totalPaginas = (int) Math.ceil((double) disponiveis.size() / ITENS_POR_PAGINA);
            int paginaAtual  = 1;

            String opcao;
            do {
                exibirPaginaCursos(disponiveis, paginaAtual, totalPaginas);
                opcao = Teclado.lerLinha().trim().toUpperCase();

                int inicio      = (paginaAtual - 1) * ITENS_POR_PAGINA;
                int fim         = Math.min(inicio + ITENS_POR_PAGINA, disponiveis.size());
                int qtdNaPagina = fim - inicio;

                if (opcao.equals("A")) {
                    if (paginaAtual > 1) paginaAtual--;
                    else System.out.println("Você já está na primeira página.");
                } else if (opcao.equals("B")) {
                    if (paginaAtual < totalPaginas) paginaAtual++;
                    else System.out.println("Você já está na última página.");
                } else if (opcao.equals("R")) {
                    return;
                } else {
                    try {
                        int num = Integer.parseInt(opcao);
                        int idxNaPagina = (num == 0) ? 9 : num - 1;
                        if (idxNaPagina >= 0 && idxNaPagina < qtdNaPagina) {
                            Curso selecionado = disponiveis.get(inicio + idxNaPagina);
                            telaDetalheCurso(selecionado, usuarioLogado,
                                    "> Início > Minhas inscrições > Lista de cursos > "
                                            + selecionado.getNome());
                        } else {
                            System.out.println("Opção inválida!");
                        }
                    } catch (NumberFormatException e) {
                        System.out.println("Opção inválida!");
                    }
                }
            } while (true);

        } catch (Exception e) {
            visao.mensagemErro("Erro ao listar cursos: " + e.getMessage());
        }
    }

    private void exibirPaginaCursos(ArrayList<Curso> cursos, int paginaAtual, int totalPaginas) {
        System.out.println("\n\nEntrePares 1.0");
        System.out.println("--------------");
        System.out.println("> Início > Minhas inscrições > Lista de cursos");
        System.out.printf("%nPágina %d de %d%n%n", paginaAtual, totalPaginas);

        int inicio = (paginaAtual - 1) * ITENS_POR_PAGINA;
        int fim    = Math.min(inicio + ITENS_POR_PAGINA, cursos.size());

        for (int i = inicio; i < fim; i++) {
            Curso curso = cursos.get(i);
            int numeroExibido = (i - inicio + 1) % ITENS_POR_PAGINA;
            System.out.printf("(%d) %s - %s%n",
                    numeroExibido, curso.getNome(), curso.getDataInicio().format(FMT));
        }

        System.out.println();
        if (paginaAtual > 1)            System.out.println("(A) Página anterior");
        if (paginaAtual < totalPaginas) System.out.println("(B) Próxima página");
        System.out.println("\n(R) Retornar ao menu anterior");
        System.out.print("\nOpção: ");
    }

    // Tela de detalhe do curso

    private void telaDetalheCurso(Curso curso, Usuario usuarioLogado, String breadcrumb) {
        String opcao;
        do {
            try {
                String nomeAutor = obterNomeAutor(curso.getIdUsuario());

                System.out.println("\n\nEntrePares 1.0");
                System.out.println("--------------");
                System.out.println(breadcrumb);
                System.out.println();
                visao.mostraCursoInscricao(curso, nomeAutor);

                boolean jaInscrito     = arquivoInscricao.existeInscricao(
                        usuarioLogado.getId(), curso.getId());
                boolean cursoAberto    = curso.getEstado() == Curso.ATIVO_INSCRICOES;
                boolean ehProprietario = curso.getIdUsuario() == usuarioLogado.getId();
                boolean podeInscrever  = cursoAberto && !ehProprietario && !jaInscrito;

                System.out.println();
                if (podeInscrever) {
                    System.out.println("(A) Fazer minha inscrição no curso");
                } else if (jaInscrito) {
                    System.out.println("Você já está inscrito neste curso.");
                } else if (ehProprietario) {
                    System.out.println("Este é um curso que você criou.");
                } else {
                    System.out.println("Este curso não está aceitando novas inscrições.");
                }

                System.out.println("\n(R) Retornar ao menu anterior");
                System.out.print("\nOpção: ");
                opcao = Teclado.lerLinha().trim().toUpperCase();

                if (opcao.equals("A") && podeInscrever) {
                    realizarInscricao(curso, usuarioLogado);
                    return;
                } else if (opcao.equals("R")) {
                    return;
                } else if (!opcao.equals("A")) {
                    System.out.println("Opção inválida!");
                }
            } catch (Exception e) {
                visao.mensagemErro("Erro: " + e.getMessage());
                return;
            }
        } while (true);
    }

    private void realizarInscricao(Curso curso, Usuario usuarioLogado) {
        try {
            arquivoInscricao.create(new Inscricao(usuarioLogado.getId(), curso.getId()));
            visao.mensagemSucesso("Inscrição no curso \"" + curso.getNome() + "\"");
        } catch (Exception e) {
            visao.mensagemErro("Não foi possível realizar a inscrição: " + e.getMessage());
        }
    }

    //Tela de gerenciamento de inscrição própria
    private void telaGerenciarInscricao(Inscricao insc, Usuario usuarioLogado) {
        String opcao;
        do {
            try {
                Curso curso = arquivoCurso.read(insc.getIdCurso());
                if (curso == null) {
                    System.out.println("Curso não encontrado.");
                    return;
                }
                String nomeAutor = obterNomeAutor(curso.getIdUsuario());

                System.out.println("\n\nEntrePares 1.0");
                System.out.println("--------------");
                System.out.println("> Início > Minhas inscrições > " + curso.getNome());
                System.out.println();
                visao.mostraCursoInscricao(curso, nomeAutor);

                System.out.println("\n(A) Cancelar minha inscrição no curso");
                System.out.println("\n(R) Retornar ao menu anterior");
                System.out.print("\nOpção: ");
                opcao = Teclado.lerLinha().trim().toUpperCase();

                if (opcao.equals("A")) {
                    cancelarInscricaoPropria(insc, curso);
                    return;
                } else if (opcao.equals("R")) {
                    return;
                } else {
                    System.out.println("Opção inválida!");
                }
            } catch (Exception e) {
                visao.mensagemErro("Erro: " + e.getMessage());
                return;
            }
        } while (true);
    }

    private void cancelarInscricaoPropria(Inscricao insc, Curso curso) {
        try {
            insc.setEstado(Inscricao.CANCELADA);
            if (arquivoInscricao.update(insc)) {
                visao.mensagemSucesso("Inscrição no curso \""
                        + curso.getNome() + "\" cancelada");
            } else {
                visao.mensagemErro("Não foi possível cancelar a inscrição");
            }
        } catch (Exception e) {
            visao.mensagemErro("Erro ao cancelar inscrição: " + e.getMessage());
        }
    }

    // Auxiliares
    private String obterNomeAutor(int idUsuario) {
        try {
            Usuario autor = arquivoUsuario.read(idUsuario);
            return (autor != null) ? autor.getNome() : "(desconhecido)";
        } catch (Exception e) {
            return "(desconhecido)";
        }
    }

    private String getLabelStatus(Curso curso) {
        switch (curso.getEstado()) {
            case Curso.ATIVO_SEM_INSCRICOES: return " (INSCRIÇÕES ENCERRADAS)";
            case Curso.CONCLUIDO:            return " (CONCLUÍDO)";
            case Curso.CANCELADO:            return " (CANCELADO)";
            default:                         return "";
        }
    }
}