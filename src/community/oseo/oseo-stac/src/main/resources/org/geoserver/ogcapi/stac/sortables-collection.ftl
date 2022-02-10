<#global pagecrumbs="<li class='breadcrumb-item'><a href='"+serviceLink("")+"'>Home</a></li><li class='breadcrumb-item'><a href='"+serviceLink("collections")+"'>Collections</a></li><li class='breadcrumb-item'><a href='"+serviceLink("collections/" + model.collectionId)+"'>"+model.collectionId+"</a></li><li class='breadcrumb-item active'>Sortables</a></li>">
<#include "common-header.ftl">
  <div class="card my-4">
    <div class="card-header">
      <h2>Sortables for ${model.collectionId}</h2>
    </div>
    <#include "sortables-common.ftl">
  </div>
    
<#include "common-footer.ftl">
