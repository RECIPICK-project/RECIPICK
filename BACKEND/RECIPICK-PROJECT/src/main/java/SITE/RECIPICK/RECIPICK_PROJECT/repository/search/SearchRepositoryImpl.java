package SITE.RECIPICK.RECIPICK_PROJECT.repository.search;

import SITE.RECIPICK.RECIPICK_PROJECT.entity.search.Ingredient;
import SITE.RECIPICK.RECIPICK_PROJECT.entity.search.RecipeIngredient;
import SITE.RECIPICK.RECIPICK_PROJECT.entity.search.SearchPost;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class SearchRepositoryImpl implements SearchRepositoryCustom {

  private final EntityManager em;

  @Override
  public Page<SearchPost> searchRecipes(List<String> mainIngredients,
      List<String> subIngredients,
      String sortType,
      Pageable pageable) {

    CriteriaBuilder cb = em.getCriteriaBuilder();
    CriteriaQuery<SearchPost> cq = cb.createQuery(SearchPost.class);
    Root<SearchPost> post = cq.from(SearchPost.class);
    Join<SearchPost, RecipeIngredient> ri = post.join("recipeIngredients", JoinType.INNER);
    Join<RecipeIngredient, Ingredient> ing = ri.join("ingredient", JoinType.INNER);

    // 조건
    List<Predicate> predicates = new ArrayList<>();
    if (mainIngredients != null && !mainIngredients.isEmpty()) {
      predicates.add(ing.get("name").in(mainIngredients));
    }

    cq.where(predicates.toArray(new Predicate[0]));
    cq.distinct(true);

    // 정렬
    List<Order> orders = new ArrayList<>();
    if ("views".equalsIgnoreCase(sortType)) {
      orders.add(cb.desc(post.get("viewCount")));
    } else if ("likes".equalsIgnoreCase(sortType)) {
      orders.add(cb.desc(post.get("likeCount")));
    } else {
      orders.add(cb.desc(post.get("createdAt"))); // 기본 최신순
    }

    cq.orderBy(orders);

    // 페이징
    TypedQuery<SearchPost> query = em.createQuery(cq);
    query.setFirstResult((int) pageable.getOffset());
    query.setMaxResults(pageable.getPageSize());
    List<SearchPost> results = query.getResultList();

    // 총 개수
    CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
    Root<SearchPost> postCount = countQuery.from(SearchPost.class);
    Join<SearchPost, RecipeIngredient> riCount = postCount.join("recipeIngredients",
        JoinType.INNER);
    Join<RecipeIngredient, Ingredient> ingCount = riCount.join("ingredient", JoinType.INNER);
    countQuery.select(cb.countDistinct(postCount));
    if (mainIngredients != null && !mainIngredients.isEmpty()) {
      countQuery.where(ingCount.get("name").in(mainIngredients));
    }
    Long total = em.createQuery(countQuery).getSingleResult();

    return new PageImpl<>(results, pageable, total);
  }
}
