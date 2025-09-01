package com.example.staj.controller;

import com.example.staj.entity.Quote;
import com.example.staj.entity.QuoteStatus;
import com.example.staj.entity.Policy;
import com.example.staj.repository.CarRepository;
import com.example.staj.repository.CustomerRepository;
import com.example.staj.repository.QuoteRepository;
import com.example.staj.services.PolicyService;
import com.example.staj.services.QuoteService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;

@Controller
public class QuoteController {

  private final QuoteService quoteService;
  private final QuoteRepository quotes;
  private final PolicyService policyService;
  private final CustomerRepository customerRepo;
  private final CarRepository carRepo;

  public QuoteController(QuoteService quoteService,
                         QuoteRepository quotes,
                         PolicyService policyService,
                         CustomerRepository customerRepo,
                         CarRepository carRepo) {
    this.quoteService = quoteService;
    this.quotes = quotes;
    this.policyService = policyService;
    this.customerRepo = customerRepo;
    this.carRepo = carRepo;
  }

  @GetMapping("/quotes")
  public String list(Model model,
                     @ModelAttribute("ok") String ok,
                     @ModelAttribute("err") String err) {
    model.addAttribute("quotes", quotes.findAll());
    if (ok != null && !ok.isBlank()) model.addAttribute("ok", ok);
    if (err != null && !err.isBlank()) model.addAttribute("err", err);
    return "list-quotes";
  }

  @GetMapping("/quotes/new")
  public String newForm(Model model) {
    model.addAttribute("quote", new Quote());
    model.addAttribute("customers", customerRepo.findAll());
    model.addAttribute("cars", carRepo.findAllByActiveTrue());
    model.addAttribute("today", LocalDate.now());
    model.addAttribute("defaultEnd", LocalDate.now().plusYears(1));
    return "add-quote";
  }

  @PostMapping("/quotes")     
  public String create(@RequestParam Long customerId,
                       @RequestParam String carPlate,
                       @RequestParam String product,
                       @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                       @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
                       @RequestParam(defaultValue = "7") int validDays,
                       RedirectAttributes ra) {
    try {
      if (startDate.isAfter(endDate)) {
        throw new IllegalArgumentException("Başlangıç tarihi, bitişten büyük olamaz.");
      }
    var car = carRepo.findByPlate(carPlate.trim())
            .orElseThrow(() -> new IllegalArgumentException("Plakaya ait araç bulunamadı: " + carPlate));

    Long carId = car.getId();

    if (!car.getCustomer().getId().equals(customerId)) {
    throw new IllegalStateException("Seçilen araç bu müşteriye ait değildir.");
}

    if (!car.isActive()) {
      throw new IllegalStateException("Araç pasif. Pasif araç için teklif oluşturulamaz.");
    }
    Quote q = quoteService.createQuote(customerId, carId, product, startDate, endDate, validDays);
    ra.addFlashAttribute("ok", "Teklif oluşturuldu: " + q.getQuoteNumber());
    return "redirect:/quotes";
    } catch (Exception ex) {
      ra.addFlashAttribute("err", ex.getMessage());
      return "redirect:/quotes/new";
    }
  }

 @PostMapping("/quotes/{id}/delete")
public String delete(@PathVariable Long id, RedirectAttributes ra) {
    try {
        Quote q = quotes.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Teklif bulunamadı: " + id));

        if (q.getStatus() != QuoteStatus.PENDING) {
            throw new IllegalStateException("Sadece onaylanmamış (PENDING) teklifler silinebilir.");
        }

        quotes.delete(q);
        ra.addFlashAttribute("ok", "Teklif silindi: " + q.getQuoteNumber());
    } catch (Exception ex) {
        ra.addFlashAttribute("err", ex.getMessage());
    }
    return "redirect:/quotes";
}
 

@PostMapping("/quotes/{id}/accept")
public String accept(@PathVariable Long id, RedirectAttributes ra) {
    try {
        Policy p = policyService.acceptFromQuote(id);  // Policy dönüyor

        String no = (p.getPolicyNumber() != null && !p.getPolicyNumber().isBlank())
                ? p.getPolicyNumber()
                : "POL-" + p.getId();

        ra.addFlashAttribute("ok", "Onaylandı. Poliçe No: " + no);

        return "redirect:/policies/" + p.getId();

    } catch (Exception ex) {
        ra.addFlashAttribute("err", ex.getMessage());
        return "redirect:/quotes";
    }
}


}
