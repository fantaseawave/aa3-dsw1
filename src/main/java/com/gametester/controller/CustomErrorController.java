package com.gametester.controller;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.http.HttpStatus; // LINHA ADICIONADA

@Controller
public class CustomErrorController implements ErrorController {

    private final MessageSource messageSource;

    public CustomErrorController(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    @RequestMapping("/error")
    public String handleError(HttpServletRequest request, Model model) {
        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        String titulo = messageSource.getMessage("error.default.title", null, LocaleContextHolder.getLocale());
        String mensagem = messageSource.getMessage("error.default.message", null, LocaleContextHolder.getLocale());

        if (status != null) {
            Integer statusCode = Integer.valueOf(status.toString());

            if(statusCode == HttpStatus.NOT_FOUND.value()) {
                titulo = messageSource.getMessage("error.404.title", null, LocaleContextHolder.getLocale());
                mensagem = messageSource.getMessage("error.404.message", null, LocaleContextHolder.getLocale());
            } else if(statusCode == HttpStatus.INTERNAL_SERVER_ERROR.value()) {
                titulo = messageSource.getMessage("error.500.title", null, LocaleContextHolder.getLocale());
                mensagem = messageSource.getMessage("error.500.message", null, LocaleContextHolder.getLocale());
            } else if(statusCode == HttpStatus.FORBIDDEN.value()) {
                titulo = messageSource.getMessage("error.403.title", null, LocaleContextHolder.getLocale());
                mensagem = messageSource.getMessage("error.403.message", null, LocaleContextHolder.getLocale());
            }
        }

        model.addAttribute("titulo", titulo);
        model.addAttribute("mensagem", mensagem);

        return "error";
    }
}