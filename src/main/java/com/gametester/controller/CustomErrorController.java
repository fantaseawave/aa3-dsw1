package com.gametester.controller;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class CustomErrorController implements ErrorController {

    @RequestMapping("/error")
    public String handleError(HttpServletRequest request, Model model) {
        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        String titulo = "Ocorreu um Erro";
        String mensagem = "Lamentamos, mas ocorreu um erro inesperado.";

        if (status != null) {
            Integer statusCode = Integer.valueOf(status.toString());

            if(statusCode == HttpStatus.NOT_FOUND.value()) {
                titulo = "Página Não Encontrada";
                mensagem = "A página que você está a procurar não existe ou foi movida.";
            } else if(statusCode == HttpStatus.INTERNAL_SERVER_ERROR.value()) {
                titulo = "Erro Interno do Servidor";
                mensagem = "Ocorreu um problema no nosso servidor. A nossa equipa já foi notificada.";
            } else if(statusCode == HttpStatus.FORBIDDEN.value()) {
                titulo = "Acesso Negado";
                mensagem = "Você não tem permissão para aceder a esta página.";
            }
        }

        model.addAttribute("titulo", titulo);
        model.addAttribute("mensagem", mensagem);

        return "error";
    }
}