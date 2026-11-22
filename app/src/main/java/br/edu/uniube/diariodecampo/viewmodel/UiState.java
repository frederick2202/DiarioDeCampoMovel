package br.edu.uniube.diariodecampo.viewmodel;

import java.util.List;
import br.edu.uniube.diariodecampo.model.RegistroCampo;

public abstract class UiState {

    public static class Idle extends UiState {
        public final List<RegistroCampo> registros;
        public Idle(List<RegistroCampo> registros) {
            this.registros = registros;
        }
    }

    public static class Loading extends UiState {
        public final String mensagem;
        public Loading(String mensagem) {
            this.mensagem = mensagem;
        }
    }

    public static class Success extends UiState {
        public final List<RegistroCampo> registros;
        public final String detalheEnriquecido;
        public Success(List<RegistroCampo> registros, String detalheEnriquecido) {
            this.registros = registros;
            this.detalheEnriquecido = detalheEnriquecido;
        }
    }

    public static class Error extends UiState {
        public final String mensagemErro;
        public final String proximoPasso;
        public Error(String mensagemErro, String proximoPasso) {
            this.mensagemErro = mensagemErro;
            this.proximoPasso = proximoPasso;
        }
    }

    public static class PermissionDenied extends UiState {
        public final String recursoNegado;
        public final String explicacao;
        public PermissionDenied(String recursoNegado, String explicacao) {
            this.recursoNegado = recursoNegado;
            this.explicacao = explicacao;
        }
    }
}
